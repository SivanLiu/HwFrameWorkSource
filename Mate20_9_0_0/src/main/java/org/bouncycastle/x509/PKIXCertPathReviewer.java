package org.bouncycastle.x509;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXParameters;
import java.security.cert.PolicyNode;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CRL;
import java.security.cert.X509CRLEntry;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.cert.X509Extension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import javax.security.auth.x500.X500Principal;
import org.bouncycastle.asn1.ASN1Enumerated;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.AuthorityInformationAccess;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DisplayText;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.GeneralSubtree;
import org.bouncycastle.asn1.x509.IssuingDistributionPoint;
import org.bouncycastle.asn1.x509.NameConstraints;
import org.bouncycastle.asn1.x509.PolicyInformation;
import org.bouncycastle.asn1.x509.qualified.MonetaryValue;
import org.bouncycastle.asn1.x509.qualified.QCStatement;
import org.bouncycastle.i18n.ErrorBundle;
import org.bouncycastle.i18n.LocaleString;
import org.bouncycastle.i18n.filter.TrustedInput;
import org.bouncycastle.i18n.filter.UntrustedInput;
import org.bouncycastle.i18n.filter.UntrustedUrlInput;
import org.bouncycastle.jce.provider.AnnotatedException;
import org.bouncycastle.jce.provider.PKIXNameConstraintValidator;
import org.bouncycastle.jce.provider.PKIXNameConstraintValidatorException;
import org.bouncycastle.jce.provider.PKIXPolicyNode;
import org.bouncycastle.util.Integers;

public class PKIXCertPathReviewer extends CertPathValidatorUtilities {
    private static final String AUTH_INFO_ACCESS = Extension.authorityInfoAccess.getId();
    private static final String CRL_DIST_POINTS = Extension.cRLDistributionPoints.getId();
    private static final String QC_STATEMENT = Extension.qCStatements.getId();
    private static final String RESOURCE_NAME = "org.bouncycastle.x509.CertPathReviewerMessages";
    protected CertPath certPath;
    protected List certs;
    protected List[] errors;
    private boolean initialized;
    protected int n;
    protected List[] notifications;
    protected PKIXParameters pkixParams;
    protected PolicyNode policyTree;
    protected PublicKey subjectPublicKey;
    protected TrustAnchor trustAnchor;
    protected Date validDate;

    public PKIXCertPathReviewer(CertPath certPath, PKIXParameters pKIXParameters) throws CertPathReviewerException {
        init(certPath, pKIXParameters);
    }

    private String IPtoString(byte[] bArr) {
        try {
            return InetAddress.getByAddress(bArr).getHostAddress();
        } catch (Exception e) {
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i != bArr.length; i++) {
                stringBuffer.append(Integer.toHexString(bArr[i] & 255));
                stringBuffer.append(' ');
            }
            return stringBuffer.toString();
        }
    }

    private void checkCriticalExtensions() {
        List<PKIXCertPathChecker> certPathCheckers = this.pkixParams.getCertPathCheckers();
        for (PKIXCertPathChecker init : certPathCheckers) {
            try {
                init.init(false);
            } catch (CertPathValidatorException e) {
                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.certPathCheckerError", new Object[]{e.getMessage(), e, e.getClass().getName()}), e);
            } catch (CertPathValidatorException e2) {
                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.criticalExtensionError", new Object[]{e2.getMessage(), e2, e2.getClass().getName()}), e2.getCause(), this.certPath, r1);
            } catch (CertPathReviewerException e3) {
                addError(e3.getErrorMessage(), e3.getIndex());
                return;
            }
        }
        int size = this.certs.size() - 1;
        while (size >= 0) {
            X509Certificate x509Certificate = (X509Certificate) this.certs.get(size);
            Set criticalExtensionOIDs = x509Certificate.getCriticalExtensionOIDs();
            if (criticalExtensionOIDs != null) {
                if (!criticalExtensionOIDs.isEmpty()) {
                    criticalExtensionOIDs.remove(KEY_USAGE);
                    criticalExtensionOIDs.remove(CERTIFICATE_POLICIES);
                    criticalExtensionOIDs.remove(POLICY_MAPPINGS);
                    criticalExtensionOIDs.remove(INHIBIT_ANY_POLICY);
                    criticalExtensionOIDs.remove(ISSUING_DISTRIBUTION_POINT);
                    criticalExtensionOIDs.remove(DELTA_CRL_INDICATOR);
                    criticalExtensionOIDs.remove(POLICY_CONSTRAINTS);
                    criticalExtensionOIDs.remove(BASIC_CONSTRAINTS);
                    criticalExtensionOIDs.remove(SUBJECT_ALTERNATIVE_NAME);
                    criticalExtensionOIDs.remove(NAME_CONSTRAINTS);
                    if (criticalExtensionOIDs.contains(QC_STATEMENT) && processQcStatements(x509Certificate, size)) {
                        criticalExtensionOIDs.remove(QC_STATEMENT);
                    }
                    for (PKIXCertPathChecker check : certPathCheckers) {
                        check.check(x509Certificate, criticalExtensionOIDs);
                    }
                    if (!criticalExtensionOIDs.isEmpty()) {
                        Iterator it = criticalExtensionOIDs.iterator();
                        while (it.hasNext()) {
                            addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.unknownCriticalExt", new Object[]{new ASN1ObjectIdentifier((String) it.next())}), size);
                        }
                    }
                }
            }
            size--;
        }
    }

    private void checkNameConstraints() {
        PKIXNameConstraintValidator pKIXNameConstraintValidator = new PKIXNameConstraintValidator();
        for (int size = this.certs.size() - 1; size > 0; size--) {
            int i = this.n;
            X509Certificate x509Certificate = (X509Certificate) this.certs.get(size);
            int i2 = 0;
            if (!CertPathValidatorUtilities.isSelfIssued(x509Certificate)) {
                GeneralName instance;
                try {
                    ASN1Sequence aSN1Sequence = (ASN1Sequence) new ASN1InputStream(new ByteArrayInputStream(CertPathValidatorUtilities.getSubjectPrincipal(x509Certificate).getEncoded())).readObject();
                    pKIXNameConstraintValidator.checkPermittedDN(aSN1Sequence);
                    pKIXNameConstraintValidator.checkExcludedDN(aSN1Sequence);
                    ASN1Sequence aSN1Sequence2 = (ASN1Sequence) CertPathValidatorUtilities.getExtensionValue(x509Certificate, SUBJECT_ALTERNATIVE_NAME);
                    if (aSN1Sequence2 != null) {
                        for (int i3 = 0; i3 < aSN1Sequence2.size(); i3++) {
                            instance = GeneralName.getInstance(aSN1Sequence2.getObjectAt(i3));
                            pKIXNameConstraintValidator.checkPermitted(instance);
                            pKIXNameConstraintValidator.checkExcluded(instance);
                        }
                    }
                } catch (AnnotatedException e) {
                    throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.ncExtError"), e, this.certPath, size);
                } catch (IOException e2) {
                    throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.ncSubjectNameError", new Object[]{new UntrustedInput(r4)}), e2, this.certPath, size);
                } catch (PKIXNameConstraintValidatorException e3) {
                    throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.notPermittedDN", new Object[]{new UntrustedInput(r4.getName())}), e3, this.certPath, size);
                } catch (PKIXNameConstraintValidatorException e32) {
                    throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.excludedDN", new Object[]{new UntrustedInput(r4.getName())}), e32, this.certPath, size);
                } catch (AnnotatedException e4) {
                    throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.subjAltNameExtError"), e4, this.certPath, size);
                } catch (PKIXNameConstraintValidatorException e322) {
                    throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.notPermittedEmail", new Object[]{new UntrustedInput(instance)}), e322, this.certPath, size);
                } catch (CertPathReviewerException e5) {
                    addError(e5.getErrorMessage(), e5.getIndex());
                    return;
                }
            }
            ASN1Sequence aSN1Sequence3 = (ASN1Sequence) CertPathValidatorUtilities.getExtensionValue(x509Certificate, NAME_CONSTRAINTS);
            if (aSN1Sequence3 != null) {
                NameConstraints instance2 = NameConstraints.getInstance(aSN1Sequence3);
                GeneralSubtree[] permittedSubtrees = instance2.getPermittedSubtrees();
                if (permittedSubtrees != null) {
                    pKIXNameConstraintValidator.intersectPermittedSubtree(permittedSubtrees);
                }
                GeneralSubtree[] excludedSubtrees = instance2.getExcludedSubtrees();
                if (excludedSubtrees != null) {
                    while (i2 != excludedSubtrees.length) {
                        pKIXNameConstraintValidator.addExcludedSubtree(excludedSubtrees[i2]);
                        i2++;
                    }
                }
            }
        }
    }

    private void checkPathLength() {
        int i = this.n;
        int i2 = 0;
        for (int size = this.certs.size() - 1; size > 0; size--) {
            BasicConstraints instance;
            int i3 = this.n;
            X509Certificate x509Certificate = (X509Certificate) this.certs.get(size);
            if (!CertPathValidatorUtilities.isSelfIssued(x509Certificate)) {
                if (i <= 0) {
                    addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.pathLengthExtended"));
                }
                i--;
                i2++;
            }
            try {
                instance = BasicConstraints.getInstance(CertPathValidatorUtilities.getExtensionValue(x509Certificate, BASIC_CONSTRAINTS));
            } catch (AnnotatedException e) {
                addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.processLengthConstError"), size);
                instance = null;
            }
            if (instance != null) {
                BigInteger pathLenConstraint = instance.getPathLenConstraint();
                if (pathLenConstraint != null) {
                    i3 = pathLenConstraint.intValue();
                    if (i3 < i) {
                        i = i3;
                    }
                }
            }
        }
        addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.totalPathLength", new Object[]{Integers.valueOf(i2)}));
    }

    /* JADX WARNING: Removed duplicated region for block: B:99:0x0235 A:{Catch:{ AnnotatedException -> 0x0619, AnnotatedException -> 0x0450, AnnotatedException -> 0x0432, AnnotatedException -> 0x0420, AnnotatedException -> 0x040e, AnnotatedException -> 0x0383, CertPathValidatorException -> 0x0370, CertPathValidatorException -> 0x021b, CertPathValidatorException -> 0x00d3, CertPathReviewerException -> 0x062b }} */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x0261 A:{Catch:{ AnnotatedException -> 0x0619, AnnotatedException -> 0x0450, AnnotatedException -> 0x0432, AnnotatedException -> 0x0420, AnnotatedException -> 0x040e, AnnotatedException -> 0x0383, CertPathValidatorException -> 0x0370, CertPathValidatorException -> 0x021b, CertPathValidatorException -> 0x00d3, CertPathReviewerException -> 0x062b }} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x0235 A:{Catch:{ AnnotatedException -> 0x0619, AnnotatedException -> 0x0450, AnnotatedException -> 0x0432, AnnotatedException -> 0x0420, AnnotatedException -> 0x040e, AnnotatedException -> 0x0383, CertPathValidatorException -> 0x0370, CertPathValidatorException -> 0x021b, CertPathValidatorException -> 0x00d3, CertPathReviewerException -> 0x062b }} */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x0261 A:{Catch:{ AnnotatedException -> 0x0619, AnnotatedException -> 0x0450, AnnotatedException -> 0x0432, AnnotatedException -> 0x0420, AnnotatedException -> 0x040e, AnnotatedException -> 0x0383, CertPathValidatorException -> 0x0370, CertPathValidatorException -> 0x021b, CertPathValidatorException -> 0x00d3, CertPathReviewerException -> 0x062b }} */
    /* JADX WARNING: Removed duplicated region for block: B:53:0x0119 A:{Catch:{ AnnotatedException -> 0x0619, AnnotatedException -> 0x0450, AnnotatedException -> 0x0432, AnnotatedException -> 0x0420, AnnotatedException -> 0x040e, AnnotatedException -> 0x0383, CertPathValidatorException -> 0x0370, CertPathValidatorException -> 0x021b, CertPathValidatorException -> 0x00d3, CertPathReviewerException -> 0x062b }} */
    /* JADX WARNING: Removed duplicated region for block: B:62:0x0139 A:{Catch:{ AnnotatedException -> 0x0619, AnnotatedException -> 0x0450, AnnotatedException -> 0x0432, AnnotatedException -> 0x0420, AnnotatedException -> 0x040e, AnnotatedException -> 0x0383, CertPathValidatorException -> 0x0370, CertPathValidatorException -> 0x021b, CertPathValidatorException -> 0x00d3, CertPathReviewerException -> 0x062b }} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x0235 A:{Catch:{ AnnotatedException -> 0x0619, AnnotatedException -> 0x0450, AnnotatedException -> 0x0432, AnnotatedException -> 0x0420, AnnotatedException -> 0x040e, AnnotatedException -> 0x0383, CertPathValidatorException -> 0x0370, CertPathValidatorException -> 0x021b, CertPathValidatorException -> 0x00d3, CertPathReviewerException -> 0x062b }} */
    /* JADX WARNING: Removed duplicated region for block: B:111:0x0261 A:{Catch:{ AnnotatedException -> 0x0619, AnnotatedException -> 0x0450, AnnotatedException -> 0x0432, AnnotatedException -> 0x0420, AnnotatedException -> 0x040e, AnnotatedException -> 0x0383, CertPathValidatorException -> 0x0370, CertPathValidatorException -> 0x021b, CertPathValidatorException -> 0x00d3, CertPathReviewerException -> 0x062b }} */
    /* JADX WARNING: Removed duplicated region for block: B:181:0x03bc A:{Catch:{ AnnotatedException -> 0x0619, AnnotatedException -> 0x0450, AnnotatedException -> 0x0432, AnnotatedException -> 0x0420, AnnotatedException -> 0x040e, AnnotatedException -> 0x0383, CertPathValidatorException -> 0x0370, CertPathValidatorException -> 0x021b, CertPathValidatorException -> 0x00d3, CertPathReviewerException -> 0x062b }} */
    /* JADX WARNING: Removed duplicated region for block: B:196:0x0400 A:{Catch:{ AnnotatedException -> 0x0619, AnnotatedException -> 0x0450, AnnotatedException -> 0x0432, AnnotatedException -> 0x0420, AnnotatedException -> 0x040e, AnnotatedException -> 0x0383, CertPathValidatorException -> 0x0370, CertPathValidatorException -> 0x021b, CertPathValidatorException -> 0x00d3, CertPathReviewerException -> 0x062b }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void checkPolicy() {
        int i;
        Set initialPolicies = this.pkixParams.getInitialPolicies();
        ArrayList[] arrayListArr = new ArrayList[(this.n + 1)];
        for (i = 0; i < arrayListArr.length; i++) {
            arrayListArr[i] = new ArrayList();
        }
        HashSet hashSet = new HashSet();
        hashSet.add(RFC3280CertPathUtilities.ANY_POLICY);
        PKIXPolicyNode pKIXPolicyNode = new PKIXPolicyNode(new ArrayList(), 0, hashSet, null, new HashSet(), RFC3280CertPathUtilities.ANY_POLICY, false);
        arrayListArr[0].add(pKIXPolicyNode);
        int i2 = this.pkixParams.isExplicitPolicyRequired() ? 0 : this.n + 1;
        int size;
        try {
            Set set;
            PKIXPolicyNode pKIXPolicyNode2;
            int i3;
            List list;
            int i4;
            PKIXPolicyNode pKIXPolicyNode3;
            Enumeration objects;
            PKIXPolicyNode pKIXPolicyNode4;
            ASN1Sequence aSN1Sequence;
            int i5;
            int intValue;
            int i6;
            size = this.certs.size() - 1;
            int i7 = this.pkixParams.isAnyPolicyInhibited() ? 0 : this.n + 1;
            int i8 = this.pkixParams.isPolicyMappingInhibited() ? 0 : this.n + 1;
            Set set2 = null;
            PKIXPolicyNode pKIXPolicyNode5 = pKIXPolicyNode;
            X509Certificate x509Certificate = null;
            while (size >= 0) {
                int i9;
                ASN1Sequence aSN1Sequence2;
                X509Extension x509Extension;
                i = this.n - size;
                X509Certificate x509Certificate2 = (X509Certificate) this.certs.get(size);
                ASN1Sequence aSN1Sequence3 = (ASN1Sequence) CertPathValidatorUtilities.getExtensionValue(x509Certificate2, CERTIFICATE_POLICIES);
                if (aSN1Sequence3 == null || pKIXPolicyNode5 == null) {
                    set = initialPolicies;
                    pKIXPolicyNode2 = pKIXPolicyNode5;
                    i9 = i7;
                    aSN1Sequence2 = aSN1Sequence3;
                    x509Extension = x509Certificate2;
                } else {
                    HashSet hashSet2;
                    HashSet hashSet3;
                    Enumeration objects2 = aSN1Sequence3.getObjects();
                    HashSet hashSet4 = new HashSet();
                    while (objects2.hasMoreElements()) {
                        PolicyInformation instance = PolicyInformation.getInstance(objects2.nextElement());
                        ASN1ObjectIdentifier policyIdentifier = instance.getPolicyIdentifier();
                        Enumeration enumeration = objects2;
                        hashSet4.add(policyIdentifier.getId());
                        set = initialPolicies;
                        if (!RFC3280CertPathUtilities.ANY_POLICY.equals(policyIdentifier.getId())) {
                            initialPolicies = CertPathValidatorUtilities.getQualifierSet(instance.getPolicyQualifiers());
                            if (!CertPathValidatorUtilities.processCertD1i(i, arrayListArr, policyIdentifier, initialPolicies)) {
                                CertPathValidatorUtilities.processCertD1ii(i, arrayListArr, policyIdentifier, initialPolicies);
                            }
                        }
                        objects2 = enumeration;
                        initialPolicies = set;
                    }
                    set = initialPolicies;
                    if (set2 != null) {
                        if (!set2.contains(RFC3280CertPathUtilities.ANY_POLICY)) {
                            hashSet2 = new HashSet();
                            for (Object next : set2) {
                                if (hashSet4.contains(next)) {
                                    hashSet2.add(next);
                                }
                            }
                            if (i7 <= 0) {
                                if (i < this.n && CertPathValidatorUtilities.isSelfIssued(x509Certificate2)) {
                                }
                                hashSet3 = hashSet2;
                                pKIXPolicyNode2 = pKIXPolicyNode5;
                                i9 = i7;
                                aSN1Sequence2 = aSN1Sequence3;
                                x509Extension = x509Certificate2;
                                for (i3 = i - 1; i3 >= 0; i3--) {
                                    list = arrayListArr[i3];
                                    pKIXPolicyNode5 = pKIXPolicyNode2;
                                    for (i4 = 0; i4 < list.size(); i4++) {
                                        pKIXPolicyNode3 = (PKIXPolicyNode) list.get(i4);
                                        if (!pKIXPolicyNode3.hasChildren()) {
                                            pKIXPolicyNode5 = CertPathValidatorUtilities.removePolicyNode(pKIXPolicyNode5, arrayListArr, pKIXPolicyNode3);
                                            if (pKIXPolicyNode5 == null) {
                                                pKIXPolicyNode2 = pKIXPolicyNode5;
                                            }
                                        }
                                    }
                                    pKIXPolicyNode2 = pKIXPolicyNode5;
                                }
                                initialPolicies = x509Extension.getCriticalExtensionOIDs();
                                if (initialPolicies != null) {
                                    boolean contains = initialPolicies.contains(CERTIFICATE_POLICIES);
                                    list = arrayListArr[i];
                                    for (i4 = 0; i4 < list.size(); i4++) {
                                        ((PKIXPolicyNode) list.get(i4)).setCritical(contains);
                                    }
                                }
                                set2 = hashSet3;
                            }
                            objects = aSN1Sequence3.getObjects();
                            while (objects.hasMoreElements()) {
                                PolicyInformation instance2 = PolicyInformation.getInstance(objects.nextElement());
                                if (RFC3280CertPathUtilities.ANY_POLICY.equals(instance2.getPolicyIdentifier().getId())) {
                                    initialPolicies = CertPathValidatorUtilities.getQualifierSet(instance2.getPolicyQualifiers());
                                    List list2 = arrayListArr[i - 1];
                                    int i10 = 0;
                                    while (i10 < list2.size()) {
                                        List list3;
                                        X509Certificate x509Certificate3;
                                        pKIXPolicyNode4 = (PKIXPolicyNode) list2.get(i10);
                                        Iterator it = pKIXPolicyNode4.getExpectedPolicies().iterator();
                                        while (it.hasNext()) {
                                            String str;
                                            Iterator it2;
                                            hashSet3 = hashSet2;
                                            Object next2 = it.next();
                                            list3 = list2;
                                            if (next2 instanceof String) {
                                                str = (String) next2;
                                            } else if (next2 instanceof ASN1ObjectIdentifier) {
                                                str = ((ASN1ObjectIdentifier) next2).getId();
                                            } else {
                                                i9 = i7;
                                                hashSet2 = hashSet3;
                                                list2 = list3;
                                            }
                                            Iterator children = pKIXPolicyNode4.getChildren();
                                            Object obj = null;
                                            while (children.hasNext()) {
                                                Iterator it3 = children;
                                                if (str.equals(((PKIXPolicyNode) children.next()).getValidPolicy())) {
                                                    obj = 1;
                                                }
                                                children = it3;
                                            }
                                            if (obj == null) {
                                                HashSet hashSet5 = new HashSet();
                                                hashSet5.add(str);
                                                pKIXPolicyNode2 = pKIXPolicyNode5;
                                                it2 = it;
                                                aSN1Sequence2 = aSN1Sequence3;
                                                i9 = i7;
                                                x509Certificate3 = x509Certificate2;
                                                PKIXPolicyNode pKIXPolicyNode6 = new PKIXPolicyNode(new ArrayList(), i, hashSet5, pKIXPolicyNode4, initialPolicies, str, false);
                                                pKIXPolicyNode4.addChild(pKIXPolicyNode6);
                                                arrayListArr[i].add(pKIXPolicyNode6);
                                            } else {
                                                pKIXPolicyNode2 = pKIXPolicyNode5;
                                                i9 = i7;
                                                it2 = it;
                                                aSN1Sequence2 = aSN1Sequence3;
                                                x509Certificate3 = x509Certificate2;
                                            }
                                            x509Certificate2 = x509Certificate3;
                                            it = it2;
                                            hashSet2 = hashSet3;
                                            list2 = list3;
                                            aSN1Sequence3 = aSN1Sequence2;
                                            pKIXPolicyNode5 = pKIXPolicyNode2;
                                            i7 = i9;
                                        }
                                        hashSet3 = hashSet2;
                                        list3 = list2;
                                        pKIXPolicyNode2 = pKIXPolicyNode5;
                                        i9 = i7;
                                        aSN1Sequence2 = aSN1Sequence3;
                                        x509Certificate3 = x509Certificate2;
                                        i10++;
                                        i7 = i9;
                                    }
                                    hashSet3 = hashSet2;
                                    pKIXPolicyNode2 = pKIXPolicyNode5;
                                    i9 = i7;
                                    aSN1Sequence2 = aSN1Sequence3;
                                    x509Extension = x509Certificate2;
                                    while (i3 >= 0) {
                                    }
                                    initialPolicies = x509Extension.getCriticalExtensionOIDs();
                                    if (initialPolicies != null) {
                                    }
                                    set2 = hashSet3;
                                } else {
                                    i9 = i7;
                                }
                            }
                            hashSet3 = hashSet2;
                            pKIXPolicyNode2 = pKIXPolicyNode5;
                            i9 = i7;
                            aSN1Sequence2 = aSN1Sequence3;
                            x509Extension = x509Certificate2;
                            while (i3 >= 0) {
                            }
                            initialPolicies = x509Extension.getCriticalExtensionOIDs();
                            if (initialPolicies != null) {
                            }
                            set2 = hashSet3;
                        }
                    }
                    hashSet2 = hashSet4;
                    if (i7 <= 0) {
                    }
                    objects = aSN1Sequence3.getObjects();
                    while (objects.hasMoreElements()) {
                    }
                    hashSet3 = hashSet2;
                    pKIXPolicyNode2 = pKIXPolicyNode5;
                    i9 = i7;
                    aSN1Sequence2 = aSN1Sequence3;
                    x509Extension = x509Certificate2;
                    while (i3 >= 0) {
                    }
                    initialPolicies = x509Extension.getCriticalExtensionOIDs();
                    if (initialPolicies != null) {
                    }
                    set2 = hashSet3;
                }
                pKIXPolicyNode4 = pKIXPolicyNode2;
                if (aSN1Sequence2 == null) {
                    pKIXPolicyNode4 = null;
                }
                if (i2 <= 0) {
                    if (pKIXPolicyNode4 == null) {
                        throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noValidPolicyTree"));
                    }
                }
                if (i != this.n) {
                    ASN1Sequence aSN1Sequence4;
                    ASN1Integer aSN1Integer;
                    ASN1Primitive extensionValue = CertPathValidatorUtilities.getExtensionValue(x509Extension, POLICY_MAPPINGS);
                    if (extensionValue != null) {
                        aSN1Sequence4 = (ASN1Sequence) extensionValue;
                        i4 = 0;
                        while (i4 < aSN1Sequence4.size()) {
                            ASN1Sequence aSN1Sequence5 = (ASN1Sequence) aSN1Sequence4.getObjectAt(i4);
                            ASN1ObjectIdentifier aSN1ObjectIdentifier = (ASN1ObjectIdentifier) aSN1Sequence5.getObjectAt(0);
                            ASN1ObjectIdentifier aSN1ObjectIdentifier2 = (ASN1ObjectIdentifier) aSN1Sequence5.getObjectAt(1);
                            if (RFC3280CertPathUtilities.ANY_POLICY.equals(aSN1ObjectIdentifier.getId())) {
                                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.invalidPolicyMapping"), this.certPath, size);
                            } else if (RFC3280CertPathUtilities.ANY_POLICY.equals(aSN1ObjectIdentifier2.getId())) {
                                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.invalidPolicyMapping"), this.certPath, size);
                            } else {
                                i4++;
                            }
                        }
                    }
                    if (extensionValue != null) {
                        aSN1Sequence = (ASN1Sequence) extensionValue;
                        HashMap hashMap = new HashMap();
                        HashSet<String> hashSet6 = new HashSet();
                        for (i5 = 0; i5 < aSN1Sequence.size(); i5++) {
                            ASN1Sequence aSN1Sequence6 = (ASN1Sequence) aSN1Sequence.getObjectAt(i5);
                            String id = ((ASN1ObjectIdentifier) aSN1Sequence6.getObjectAt(0)).getId();
                            String id2 = ((ASN1ObjectIdentifier) aSN1Sequence6.getObjectAt(1)).getId();
                            if (hashMap.containsKey(id)) {
                                ((Set) hashMap.get(id)).add(id2);
                            } else {
                                HashSet hashSet7 = new HashSet();
                                hashSet7.add(id2);
                                hashMap.put(id, hashSet7);
                                hashSet6.add(id);
                            }
                        }
                        for (String str2 : hashSet6) {
                            if (i8 > 0) {
                                CertPathValidatorUtilities.prepareNextCertB1(i, arrayListArr, str2, hashMap, x509Extension);
                            } else if (i8 <= 0) {
                                pKIXPolicyNode4 = CertPathValidatorUtilities.prepareNextCertB2(i, arrayListArr, str2, pKIXPolicyNode4);
                            }
                        }
                    }
                    if (!CertPathValidatorUtilities.isSelfIssued(x509Extension)) {
                        if (i2 != 0) {
                            i2--;
                        }
                        if (i8 != 0) {
                            i8--;
                        }
                        if (i9 != 0) {
                            i3 = i9 - 1;
                            aSN1Sequence4 = (ASN1Sequence) CertPathValidatorUtilities.getExtensionValue(x509Extension, POLICY_CONSTRAINTS);
                            if (aSN1Sequence4 != null) {
                                Enumeration objects3 = aSN1Sequence4.getObjects();
                                while (objects3.hasMoreElements()) {
                                    ASN1TaggedObject aSN1TaggedObject = (ASN1TaggedObject) objects3.nextElement();
                                    switch (aSN1TaggedObject.getTagNo()) {
                                        case 0:
                                            i4 = ASN1Integer.getInstance(aSN1TaggedObject, false).getValue().intValue();
                                            if (i4 >= i2) {
                                                break;
                                            }
                                            i2 = i4;
                                            break;
                                        case 1:
                                            i4 = ASN1Integer.getInstance(aSN1TaggedObject, false).getValue().intValue();
                                            if (i4 >= i8) {
                                                break;
                                            }
                                            i8 = i4;
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                            aSN1Integer = (ASN1Integer) CertPathValidatorUtilities.getExtensionValue(x509Extension, INHIBIT_ANY_POLICY);
                            if (aSN1Integer != null) {
                                intValue = aSN1Integer.getValue().intValue();
                                if (intValue < i3) {
                                    i3 = intValue;
                                }
                            }
                            i9 = i3;
                        }
                    }
                    i3 = i9;
                    aSN1Sequence4 = (ASN1Sequence) CertPathValidatorUtilities.getExtensionValue(x509Extension, POLICY_CONSTRAINTS);
                    if (aSN1Sequence4 != null) {
                    }
                    aSN1Integer = (ASN1Integer) CertPathValidatorUtilities.getExtensionValue(x509Extension, INHIBIT_ANY_POLICY);
                    if (aSN1Integer != null) {
                    }
                    i9 = i3;
                }
                pKIXPolicyNode5 = pKIXPolicyNode4;
                size--;
                X509Extension x509Certificate4 = x509Extension;
                initialPolicies = set;
                i7 = i9;
            }
            set = initialPolicies;
            pKIXPolicyNode2 = pKIXPolicyNode5;
            if (!CertPathValidatorUtilities.isSelfIssued(x509Certificate4) && i2 > 0) {
                i2--;
            }
            aSN1Sequence = (ASN1Sequence) CertPathValidatorUtilities.getExtensionValue(x509Certificate4, POLICY_CONSTRAINTS);
            if (aSN1Sequence != null) {
                objects = aSN1Sequence.getObjects();
                i4 = i2;
                while (objects.hasMoreElements()) {
                    ASN1TaggedObject aSN1TaggedObject2 = (ASN1TaggedObject) objects.nextElement();
                    if (aSN1TaggedObject2.getTagNo() == 0) {
                        if (ASN1Integer.getInstance(aSN1TaggedObject2, false).getValue().intValue() == 0) {
                            i4 = false;
                        }
                    }
                }
                i6 = 0;
            } else {
                i6 = 0;
                i4 = i2;
            }
            if (pKIXPolicyNode2 != null) {
                initialPolicies = set;
                if (CertPathValidatorUtilities.isAnyPolicy(initialPolicies)) {
                    if (this.pkixParams.isExplicitPolicyRequired()) {
                        if (set2.isEmpty()) {
                            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.explicitPolicy"), this.certPath, size);
                        }
                        HashSet<PKIXPolicyNode> hashSet8 = new HashSet();
                        for (intValue = i6; intValue < arrayListArr.length; intValue++) {
                            List list4 = arrayListArr[intValue];
                            for (i2 = i6; i2 < list4.size(); i2++) {
                                pKIXPolicyNode5 = (PKIXPolicyNode) list4.get(i2);
                                if (RFC3280CertPathUtilities.ANY_POLICY.equals(pKIXPolicyNode5.getValidPolicy())) {
                                    Iterator children2 = pKIXPolicyNode5.getChildren();
                                    while (children2.hasNext()) {
                                        hashSet8.add(children2.next());
                                    }
                                }
                            }
                        }
                        for (PKIXPolicyNode validPolicy : hashSet8) {
                            set2.contains(validPolicy.getValidPolicy());
                        }
                        if (pKIXPolicyNode2 != null) {
                            i3 = this.n - 1;
                            while (i3 >= 0) {
                                list = arrayListArr[i3];
                                pKIXPolicyNode = pKIXPolicyNode2;
                                for (i = i6; i < list.size(); i++) {
                                    pKIXPolicyNode5 = (PKIXPolicyNode) list.get(i);
                                    if (!pKIXPolicyNode5.hasChildren()) {
                                        pKIXPolicyNode = CertPathValidatorUtilities.removePolicyNode(pKIXPolicyNode, arrayListArr, pKIXPolicyNode5);
                                    }
                                }
                                i3--;
                                pKIXPolicyNode2 = pKIXPolicyNode;
                            }
                        }
                    }
                    pKIXPolicyNode4 = pKIXPolicyNode2;
                } else {
                    HashSet<PKIXPolicyNode> hashSet9 = new HashSet();
                    for (i = i6; i < arrayListArr.length; i++) {
                        List list5 = arrayListArr[i];
                        for (i5 = i6; i5 < list5.size(); i5++) {
                            pKIXPolicyNode3 = (PKIXPolicyNode) list5.get(i5);
                            if (RFC3280CertPathUtilities.ANY_POLICY.equals(pKIXPolicyNode3.getValidPolicy())) {
                                Iterator children3 = pKIXPolicyNode3.getChildren();
                                while (children3.hasNext()) {
                                    PKIXPolicyNode pKIXPolicyNode7 = (PKIXPolicyNode) children3.next();
                                    if (!RFC3280CertPathUtilities.ANY_POLICY.equals(pKIXPolicyNode7.getValidPolicy())) {
                                        hashSet9.add(pKIXPolicyNode7);
                                    }
                                }
                            }
                        }
                    }
                    PKIXPolicyNode pKIXPolicyNode8 = pKIXPolicyNode2;
                    for (PKIXPolicyNode pKIXPolicyNode9 : hashSet9) {
                        if (!initialPolicies.contains(pKIXPolicyNode9.getValidPolicy())) {
                            pKIXPolicyNode8 = CertPathValidatorUtilities.removePolicyNode(pKIXPolicyNode8, arrayListArr, pKIXPolicyNode9);
                        }
                    }
                    if (pKIXPolicyNode8 != null) {
                        i3 = this.n - 1;
                        while (i3 >= 0) {
                            list = arrayListArr[i3];
                            pKIXPolicyNode9 = pKIXPolicyNode8;
                            for (i = i6; i < list.size(); i++) {
                                pKIXPolicyNode5 = (PKIXPolicyNode) list.get(i);
                                if (!pKIXPolicyNode5.hasChildren()) {
                                    pKIXPolicyNode9 = CertPathValidatorUtilities.removePolicyNode(pKIXPolicyNode9, arrayListArr, pKIXPolicyNode5);
                                }
                            }
                            i3--;
                            pKIXPolicyNode8 = pKIXPolicyNode9;
                        }
                    }
                    pKIXPolicyNode4 = pKIXPolicyNode8;
                }
            } else if (this.pkixParams.isExplicitPolicyRequired()) {
                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.explicitPolicy"), this.certPath, size);
            } else {
                pKIXPolicyNode4 = null;
            }
            if (i4 <= 0 && r10 == null) {
                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.invalidPolicy"));
            }
        } catch (AnnotatedException e) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.policyConstExtError"), this.certPath, size);
        } catch (AnnotatedException e2) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.policyExtError"), e2, this.certPath, size);
        } catch (AnnotatedException e22) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.policyMapExtError"), e22, this.certPath, size);
        } catch (AnnotatedException e3) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.policyConstExtError"), this.certPath, size);
        } catch (AnnotatedException e4) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.policyInhibitExtError"), this.certPath, size);
        } catch (AnnotatedException e5) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.policyExtError"), e5, this.certPath, size);
        } catch (CertPathValidatorException e52) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.policyQualifierError"), e52, this.certPath, size);
        } catch (CertPathValidatorException e6) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.policyQualifierError"), e6, this.certPath, size);
        } catch (CertPathValidatorException e62) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.policyQualifierError"), e62, this.certPath, size);
        } catch (CertPathReviewerException e7) {
            addError(e7.getErrorMessage(), e7.getIndex());
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:108:0x0301 A:{LOOP_END, LOOP:1: B:106:0x02fb->B:108:0x0301} */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x032e A:{LOOP_END, LOOP:2: B:110:0x0328->B:112:0x032e} */
    /* JADX WARNING: Removed duplicated region for block: B:101:0x02dc A:{Catch:{ AnnotatedException -> 0x02e1 }} */
    /* JADX WARNING: Removed duplicated region for block: B:108:0x0301 A:{LOOP_END, LOOP:1: B:106:0x02fb->B:108:0x0301} */
    /* JADX WARNING: Removed duplicated region for block: B:112:0x032e A:{LOOP_END, LOOP:2: B:110:0x0328->B:112:0x032e} */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0153  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0108  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x0180  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0156  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x018f  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x03af A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x037c  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x03af A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0108  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0153  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0156  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x0180  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x018f  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0153  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0108  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x0180  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0156  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x018f  */
    /* JADX WARNING: Removed duplicated region for block: B:31:0x0108  */
    /* JADX WARNING: Removed duplicated region for block: B:46:0x0153  */
    /* JADX WARNING: Removed duplicated region for block: B:48:0x0156  */
    /* JADX WARNING: Removed duplicated region for block: B:57:0x0180  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x018f  */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x0375  */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x02b6 A:{SYNTHETIC, Splitter:B:90:0x02b6} */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x037c  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x03af A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:90:0x02b6 A:{SYNTHETIC, Splitter:B:90:0x02b6} */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x0375  */
    /* JADX WARNING: Removed duplicated region for block: B:125:0x037c  */
    /* JADX WARNING: Removed duplicated region for block: B:131:0x03af A:{SKIP} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void checkSignatures() {
        TrustAnchor trustAnchor;
        CertPathReviewerException e;
        Throwable th;
        TrustAnchor trustAnchor2;
        int size;
        X500Principal x500Principal;
        X500Principal x500Principal2;
        X509Certificate x509Certificate;
        X509Certificate trustedCert;
        PublicKey publicKey;
        PublicKey publicKey2;
        PublicKey publicKey3;
        AlgorithmIdentifier algorithmIdentifier;
        X509Certificate x509Certificate2;
        ErrorBundle errorBundle;
        ErrorBundle errorBundle2;
        X509Certificate x509Certificate3;
        Object obj;
        int i;
        int i2;
        int i3;
        int i4;
        X500Principal subjectX500Principal;
        int i5 = 2;
        r3 = new Object[2];
        int i6 = 0;
        r3[0] = new TrustedInput(this.validDate);
        int i7 = 1;
        r3[1] = new TrustedInput(new Date());
        addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.certPathValidDate", r3));
        try {
            X509Certificate x509Certificate4 = (X509Certificate) this.certs.get(this.certs.size() - 1);
            Collection trustAnchors = getTrustAnchors(x509Certificate4, this.pkixParams.getTrustAnchors());
            if (trustAnchors.size() > 1) {
                addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.conflictingTrustAnchors", new Object[]{Integers.valueOf(trustAnchors.size()), new UntrustedInput(x509Certificate4.getIssuerX500Principal())}));
            } else if (trustAnchors.isEmpty()) {
                addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noTrustAnchorFound", new Object[]{new UntrustedInput(x509Certificate4.getIssuerX500Principal()), Integers.valueOf(this.pkixParams.getTrustAnchors().size())}));
            } else {
                trustAnchor = (TrustAnchor) trustAnchors.iterator().next();
                try {
                    try {
                        CertPathValidatorUtilities.verifyX509Certificate(x509Certificate4, trustAnchor.getTrustedCert() != null ? trustAnchor.getTrustedCert().getPublicKey() : trustAnchor.getCAPublicKey(), this.pkixParams.getSigProvider());
                    } catch (SignatureException e2) {
                        addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.trustButInvalidCert"));
                    } catch (Exception e3) {
                    }
                } catch (CertPathReviewerException e4) {
                    e = e4;
                } catch (Throwable th2) {
                    th = th2;
                    addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.unknown", new Object[]{new UntrustedInput(th.getMessage()), new UntrustedInput(th)}));
                    trustAnchor2 = trustAnchor;
                    if (trustAnchor2 != null) {
                    }
                    if (trustAnchor2 != null) {
                    }
                    size = this.certs.size() - 1;
                    x500Principal = x500Principal2;
                    x509Certificate = trustedCert;
                    publicKey = publicKey2;
                    while (size >= 0) {
                    }
                    publicKey3 = publicKey;
                    this.trustAnchor = trustAnchor2;
                    this.subjectPublicKey = publicKey3;
                    return;
                }
                trustAnchor2 = trustAnchor;
                if (trustAnchor2 != null) {
                    X500Principal subjectPrincipal;
                    X509Certificate trustedCert2 = trustAnchor2.getTrustedCert();
                    if (trustedCert2 != null) {
                        try {
                            subjectPrincipal = CertPathValidatorUtilities.getSubjectPrincipal(trustedCert2);
                        } catch (IllegalArgumentException e5) {
                            addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.trustDNInvalid", new Object[]{new UntrustedInput(trustAnchor2.getCAName())}));
                            subjectPrincipal = null;
                        }
                    } else {
                        subjectPrincipal = new X500Principal(trustAnchor2.getCAName());
                    }
                    if (trustedCert2 != null) {
                        boolean[] keyUsage = trustedCert2.getKeyUsage();
                        if (!(keyUsage == null || keyUsage[5])) {
                            addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.trustKeyUsage"));
                        }
                    }
                    x500Principal2 = subjectPrincipal;
                } else {
                    x500Principal2 = null;
                }
                if (trustAnchor2 != null) {
                    trustedCert = trustAnchor2.getTrustedCert();
                    publicKey2 = trustedCert != null ? trustedCert.getPublicKey() : trustAnchor2.getCAPublicKey();
                    try {
                        algorithmIdentifier = CertPathValidatorUtilities.getAlgorithmIdentifier(publicKey2);
                        algorithmIdentifier.getAlgorithm();
                        algorithmIdentifier.getParameters();
                    } catch (CertPathValidatorException e6) {
                        addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.trustPubKeyError"));
                    }
                } else {
                    trustedCert = null;
                    publicKey2 = null;
                }
                size = this.certs.size() - 1;
                x500Principal = x500Principal2;
                x509Certificate = trustedCert;
                publicKey = publicKey2;
                while (size >= 0) {
                    int i8 = this.n - size;
                    x509Certificate2 = (X509Certificate) this.certs.get(size);
                    if (publicKey != null) {
                        try {
                            CertPathValidatorUtilities.verifyX509Certificate(x509Certificate2, publicKey, this.pkixParams.getSigProvider());
                        } catch (GeneralSecurityException e7) {
                            errorBundle = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.signatureNotVerified", new Object[]{e7.getMessage(), e7, e7.getClass().getName()});
                        }
                    } else if (CertPathValidatorUtilities.isSelfIssued(x509Certificate2)) {
                        try {
                            CertPathValidatorUtilities.verifyX509Certificate(x509Certificate2, x509Certificate2.getPublicKey(), this.pkixParams.getSigProvider());
                            addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.rootKeyIsValidButNotATrustAnchor"), size);
                        } catch (GeneralSecurityException e72) {
                            errorBundle = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.signatureNotVerified", new Object[]{e72.getMessage(), e72, e72.getClass().getName()});
                        }
                    } else {
                        errorBundle2 = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.NoIssuerPublicKey");
                        byte[] extensionValue = x509Certificate2.getExtensionValue(Extension.authorityKeyIdentifier.getId());
                        if (extensionValue != null) {
                            AuthorityKeyIdentifier instance = AuthorityKeyIdentifier.getInstance(ASN1OctetString.getInstance(extensionValue).getOctets());
                            GeneralNames authorityCertIssuer = instance.getAuthorityCertIssuer();
                            if (authorityCertIssuer != null) {
                                Object obj2 = authorityCertIssuer.getNames()[i6];
                                if (instance.getAuthorityCertSerialNumber() != null) {
                                    errorBundle2.setExtraArguments(new Object[]{new LocaleString(RESOURCE_NAME, "missingIssuer"), " \"", obj2, "\" ", new LocaleString(RESOURCE_NAME, "missingSerial"), " ", instance.getAuthorityCertSerialNumber()});
                                }
                            }
                        }
                        addError(errorBundle2, size);
                    }
                    try {
                        x509Certificate2.checkValidity(this.validDate);
                    } catch (CertificateNotYetValidException e8) {
                        errorBundle2 = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.certificateNotYetValid", new Object[]{new TrustedInput(x509Certificate2.getNotBefore())});
                    } catch (CertificateExpiredException e9) {
                        errorBundle2 = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.certificateExpired", new Object[]{new TrustedInput(x509Certificate2.getNotAfter())});
                    }
                    if (this.pkixParams.isRevocationEnabled()) {
                        ASN1Primitive extensionValue2;
                        CRLDistPoint instance2;
                        AuthorityInformationAccess instance3;
                        Vector cRLDistUrls;
                        Vector oCSPUrls;
                        Iterator it;
                        int i9;
                        Vector vector;
                        int i10;
                        try {
                            extensionValue2 = CertPathValidatorUtilities.getExtensionValue(x509Certificate2, CRL_DIST_POINTS);
                            if (extensionValue2 != null) {
                                instance2 = CRLDistPoint.getInstance(extensionValue2);
                                extensionValue2 = CertPathValidatorUtilities.getExtensionValue(x509Certificate2, AUTH_INFO_ACCESS);
                                if (extensionValue2 != null) {
                                    instance3 = AuthorityInformationAccess.getInstance(extensionValue2);
                                    cRLDistUrls = getCRLDistUrls(instance2);
                                    oCSPUrls = getOCSPUrls(instance3);
                                    it = cRLDistUrls.iterator();
                                    while (it.hasNext()) {
                                        Object[] objArr = new Object[1];
                                        i9 = i8;
                                        objArr[0] = new UntrustedUrlInput(it.next());
                                        addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlDistPoint", objArr), size);
                                        i8 = i9;
                                    }
                                    i9 = i8;
                                    it = oCSPUrls.iterator();
                                    while (it.hasNext()) {
                                        addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.ocspLocation", new Object[]{new UntrustedUrlInput(it.next())}), size);
                                    }
                                    x509Certificate3 = x509Certificate2;
                                    i7 = i9;
                                    obj = x500Principal;
                                    vector = cRLDistUrls;
                                    publicKey3 = publicKey;
                                    i10 = size;
                                    checkRevocation(this.pkixParams, x509Certificate2, this.validDate, x509Certificate, publicKey, vector, oCSPUrls, size);
                                    i = i10;
                                }
                                instance3 = null;
                                cRLDistUrls = getCRLDistUrls(instance2);
                                oCSPUrls = getOCSPUrls(instance3);
                                it = cRLDistUrls.iterator();
                                while (it.hasNext()) {
                                }
                                i9 = i8;
                                it = oCSPUrls.iterator();
                                while (it.hasNext()) {
                                }
                                x509Certificate3 = x509Certificate2;
                                i7 = i9;
                                obj = x500Principal;
                                vector = cRLDistUrls;
                                publicKey3 = publicKey;
                                i10 = size;
                                try {
                                    checkRevocation(this.pkixParams, x509Certificate2, this.validDate, x509Certificate, publicKey, vector, oCSPUrls, size);
                                    i = i10;
                                } catch (CertPathReviewerException e10) {
                                    e = e10;
                                    i = i10;
                                    addError(e.getErrorMessage(), i);
                                    if (obj != null) {
                                    }
                                    i2 = 2;
                                    i3 = 0;
                                    i4 = 1;
                                    if (i7 != this.n) {
                                    }
                                    subjectX500Principal = x509Certificate3.getSubjectX500Principal();
                                    publicKey2 = CertPathValidatorUtilities.getNextWorkingKey(this.certs, i);
                                    try {
                                        algorithmIdentifier = CertPathValidatorUtilities.getAlgorithmIdentifier(publicKey2);
                                        algorithmIdentifier.getAlgorithm();
                                        algorithmIdentifier.getParameters();
                                    } catch (CertPathValidatorException e11) {
                                    }
                                    size = i - 1;
                                    i5 = i2;
                                    i7 = i4;
                                    x509Certificate = x509Certificate3;
                                    publicKey = publicKey2;
                                    i6 = i3;
                                    x500Principal = subjectX500Principal;
                                }
                            }
                        } catch (AnnotatedException e12) {
                            addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlDistPtExtError"), size);
                        }
                        instance2 = null;
                        try {
                            extensionValue2 = CertPathValidatorUtilities.getExtensionValue(x509Certificate2, AUTH_INFO_ACCESS);
                            if (extensionValue2 != null) {
                            }
                        } catch (AnnotatedException e13) {
                            addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlAuthInfoAccError"), size);
                        }
                        instance3 = null;
                        cRLDistUrls = getCRLDistUrls(instance2);
                        oCSPUrls = getOCSPUrls(instance3);
                        it = cRLDistUrls.iterator();
                        while (it.hasNext()) {
                        }
                        i9 = i8;
                        it = oCSPUrls.iterator();
                        while (it.hasNext()) {
                        }
                        try {
                            x509Certificate3 = x509Certificate2;
                            i7 = i9;
                            obj = x500Principal;
                            vector = cRLDistUrls;
                            publicKey3 = publicKey;
                            i10 = size;
                            checkRevocation(this.pkixParams, x509Certificate2, this.validDate, x509Certificate, publicKey, vector, oCSPUrls, size);
                            i = i10;
                        } catch (CertPathReviewerException e14) {
                            e = e14;
                            x509Certificate3 = x509Certificate2;
                            obj = x500Principal;
                            publicKey3 = publicKey;
                            i10 = size;
                            i7 = i9;
                            i = i10;
                            addError(e.getErrorMessage(), i);
                            if (obj != null) {
                            }
                            i2 = 2;
                            i3 = 0;
                            i4 = 1;
                            if (i7 != this.n) {
                            }
                            subjectX500Principal = x509Certificate3.getSubjectX500Principal();
                            publicKey2 = CertPathValidatorUtilities.getNextWorkingKey(this.certs, i);
                            algorithmIdentifier = CertPathValidatorUtilities.getAlgorithmIdentifier(publicKey2);
                            algorithmIdentifier.getAlgorithm();
                            algorithmIdentifier.getParameters();
                            size = i - 1;
                            i5 = i2;
                            i7 = i4;
                            x509Certificate = x509Certificate3;
                            publicKey = publicKey2;
                            i6 = i3;
                            x500Principal = subjectX500Principal;
                        }
                    } else {
                        x509Certificate3 = x509Certificate2;
                        i7 = i8;
                        obj = x500Principal;
                        publicKey3 = publicKey;
                        i = size;
                    }
                    if (obj != null || x509Certificate3.getIssuerX500Principal().equals(obj)) {
                        i2 = 2;
                        i3 = 0;
                        i4 = 1;
                    } else {
                        i2 = 2;
                        r5 = new Object[2];
                        i3 = 0;
                        r5[0] = obj.getName();
                        i4 = 1;
                        r5[1] = x509Certificate3.getIssuerX500Principal().getName();
                        addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.certWrongIssuer", r5), i);
                    }
                    if (i7 != this.n) {
                        boolean[] keyUsage2;
                        if (x509Certificate3 != null && x509Certificate3.getVersion() == i4) {
                            addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noCACert"), i);
                        }
                        try {
                            BasicConstraints instance4 = BasicConstraints.getInstance(CertPathValidatorUtilities.getExtensionValue(x509Certificate3, BASIC_CONSTRAINTS));
                            if (instance4 != null) {
                                if (!instance4.isCA()) {
                                    errorBundle2 = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noCACert");
                                }
                                keyUsage2 = x509Certificate3.getKeyUsage();
                                if (!(keyUsage2 == null || keyUsage2[5])) {
                                    addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noCertSign"), i);
                                }
                            } else {
                                errorBundle2 = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noBasicConstraints");
                            }
                            addError(errorBundle2, i);
                        } catch (AnnotatedException e15) {
                            addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.errorProcesingBC"), i);
                        }
                        keyUsage2 = x509Certificate3.getKeyUsage();
                        addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noCertSign"), i);
                    }
                    subjectX500Principal = x509Certificate3.getSubjectX500Principal();
                    try {
                        publicKey2 = CertPathValidatorUtilities.getNextWorkingKey(this.certs, i);
                        algorithmIdentifier = CertPathValidatorUtilities.getAlgorithmIdentifier(publicKey2);
                        algorithmIdentifier.getAlgorithm();
                        algorithmIdentifier.getParameters();
                    } catch (CertPathValidatorException e16) {
                        publicKey2 = publicKey3;
                        addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.pubKeyError"), i);
                        size = i - 1;
                        i5 = i2;
                        i7 = i4;
                        x509Certificate = x509Certificate3;
                        publicKey = publicKey2;
                        i6 = i3;
                        x500Principal = subjectX500Principal;
                    }
                    size = i - 1;
                    i5 = i2;
                    i7 = i4;
                    x509Certificate = x509Certificate3;
                    publicKey = publicKey2;
                    i6 = i3;
                    x500Principal = subjectX500Principal;
                }
                publicKey3 = publicKey;
                this.trustAnchor = trustAnchor2;
                this.subjectPublicKey = publicKey3;
                return;
            }
            trustAnchor = null;
        } catch (CertPathReviewerException e17) {
            e = e17;
            trustAnchor = null;
            addError(e.getErrorMessage());
            trustAnchor2 = trustAnchor;
            if (trustAnchor2 != null) {
            }
            if (trustAnchor2 != null) {
            }
            size = this.certs.size() - 1;
            x500Principal = x500Principal2;
            x509Certificate = trustedCert;
            publicKey = publicKey2;
            while (size >= 0) {
            }
            publicKey3 = publicKey;
            this.trustAnchor = trustAnchor2;
            this.subjectPublicKey = publicKey3;
            return;
        } catch (Throwable th3) {
            th = th3;
            trustAnchor = null;
            addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.unknown", new Object[]{new UntrustedInput(th.getMessage()), new UntrustedInput(th)}));
            trustAnchor2 = trustAnchor;
            if (trustAnchor2 != null) {
            }
            if (trustAnchor2 != null) {
            }
            size = this.certs.size() - 1;
            x500Principal = x500Principal2;
            x509Certificate = trustedCert;
            publicKey = publicKey2;
            while (size >= 0) {
            }
            publicKey3 = publicKey;
            this.trustAnchor = trustAnchor2;
            this.subjectPublicKey = publicKey3;
            return;
        }
        trustAnchor2 = trustAnchor;
        if (trustAnchor2 != null) {
        }
        if (trustAnchor2 != null) {
        }
        size = this.certs.size() - 1;
        x500Principal = x500Principal2;
        x509Certificate = trustedCert;
        publicKey = publicKey2;
        while (size >= 0) {
        }
        publicKey3 = publicKey;
        this.trustAnchor = trustAnchor2;
        this.subjectPublicKey = publicKey3;
        return;
        addError(errorBundle, size);
        x509Certificate2.checkValidity(this.validDate);
        if (this.pkixParams.isRevocationEnabled()) {
        }
        if (obj != null) {
        }
        i2 = 2;
        i3 = 0;
        i4 = 1;
        if (i7 != this.n) {
        }
        subjectX500Principal = x509Certificate3.getSubjectX500Principal();
        publicKey2 = CertPathValidatorUtilities.getNextWorkingKey(this.certs, i);
        algorithmIdentifier = CertPathValidatorUtilities.getAlgorithmIdentifier(publicKey2);
        algorithmIdentifier.getAlgorithm();
        algorithmIdentifier.getParameters();
        size = i - 1;
        i5 = i2;
        i7 = i4;
        x509Certificate = x509Certificate3;
        publicKey = publicKey2;
        i6 = i3;
        x500Principal = subjectX500Principal;
        addError(errorBundle2, size);
        if (this.pkixParams.isRevocationEnabled()) {
        }
        if (obj != null) {
        }
        i2 = 2;
        i3 = 0;
        i4 = 1;
        if (i7 != this.n) {
        }
        subjectX500Principal = x509Certificate3.getSubjectX500Principal();
        publicKey2 = CertPathValidatorUtilities.getNextWorkingKey(this.certs, i);
        algorithmIdentifier = CertPathValidatorUtilities.getAlgorithmIdentifier(publicKey2);
        algorithmIdentifier.getAlgorithm();
        algorithmIdentifier.getParameters();
        size = i - 1;
        i5 = i2;
        i7 = i4;
        x509Certificate = x509Certificate3;
        publicKey = publicKey2;
        i6 = i3;
        x500Principal = subjectX500Principal;
    }

    private X509CRL getCRL(String str) throws CertPathReviewerException {
        try {
            URL url = new URL(str);
            if (!url.getProtocol().equals("http")) {
                if (!url.getProtocol().equals("https")) {
                    return null;
                }
            }
            HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.connect();
            if (httpURLConnection.getResponseCode() == DisplayText.DISPLAY_TEXT_MAXIMUM_SIZE) {
                return (X509CRL) CertificateFactory.getInstance("X.509", "BC").generateCRL(httpURLConnection.getInputStream());
            }
            throw new Exception(httpURLConnection.getResponseMessage());
        } catch (Exception e) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.loadCrlDistPointError", new Object[]{new UntrustedInput(str), e.getMessage(), e, e.getClass().getName()}));
        }
    }

    private boolean processQcStatements(X509Certificate x509Certificate, int i) {
        int i2 = i;
        int i3 = 0;
        try {
            ASN1Sequence aSN1Sequence = (ASN1Sequence) CertPathValidatorUtilities.getExtensionValue(x509Certificate, QC_STATEMENT);
            int i4 = 0;
            int i5 = i4;
            while (i4 < aSN1Sequence.size()) {
                ErrorBundle errorBundle;
                QCStatement instance = QCStatement.getInstance(aSN1Sequence.getObjectAt(i4));
                if (QCStatement.id_etsi_qcs_QcCompliance.equals(instance.getStatementId())) {
                    errorBundle = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.QcEuCompliance");
                } else {
                    if (!QCStatement.id_qcs_pkixQCSyntax_v1.equals(instance.getStatementId())) {
                        if (QCStatement.id_etsi_qcs_QcSSCD.equals(instance.getStatementId())) {
                            errorBundle = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.QcSSCD");
                        } else if (QCStatement.id_etsi_qcs_LimiteValue.equals(instance.getStatementId())) {
                            ErrorBundle errorBundle2;
                            MonetaryValue instance2 = MonetaryValue.getInstance(instance.getStatementInfo());
                            instance2.getCurrency();
                            double doubleValue = instance2.getAmount().doubleValue() * Math.pow(10.0d, instance2.getExponent().doubleValue());
                            if (instance2.getCurrency().isAlphabetic()) {
                                errorBundle2 = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.QcLimitValueAlpha", new Object[]{instance2.getCurrency().getAlphabetic(), new TrustedInput(new Double(doubleValue)), instance2});
                            } else {
                                errorBundle2 = new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.QcLimitValueNum", new Object[]{Integers.valueOf(instance2.getCurrency().getNumeric()), new TrustedInput(new Double(doubleValue)), instance2});
                            }
                            addNotification(errorBundle2, i2);
                        } else {
                            addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.QcUnknownStatement", new Object[]{instance.getStatementId(), new UntrustedInput(instance)}), i2);
                            i5 = 1;
                        }
                    }
                    i4++;
                    i3 = 0;
                }
                addNotification(errorBundle, i2);
                i4++;
                i3 = 0;
            }
            return i5 ^ 1;
        } catch (AnnotatedException e) {
            addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.QcStatementExtError"), i2);
            return false;
        }
    }

    protected void addError(ErrorBundle errorBundle) {
        this.errors[0].add(errorBundle);
    }

    protected void addError(ErrorBundle errorBundle, int i) {
        if (i < -1 || i >= this.n) {
            throw new IndexOutOfBoundsException();
        }
        this.errors[i + 1].add(errorBundle);
    }

    protected void addNotification(ErrorBundle errorBundle) {
        this.notifications[0].add(errorBundle);
    }

    protected void addNotification(ErrorBundle errorBundle, int i) {
        if (i < -1 || i >= this.n) {
            throw new IndexOutOfBoundsException();
        }
        this.notifications[i + 1].add(errorBundle);
    }

    /* JADX WARNING: Removed duplicated region for block: B:97:0x028c  */
    /* JADX WARNING: Removed duplicated region for block: B:101:0x02c2  */
    /* JADX WARNING: Removed duplicated region for block: B:100:0x02a3  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected void checkCRLs(PKIXParameters pKIXParameters, X509Certificate x509Certificate, Date date, X509Certificate x509Certificate2, PublicKey publicKey, Vector vector, int i) throws CertPathReviewerException {
        CertPathReviewerException e;
        int i2;
        PKIXParameters pKIXParameters2 = pKIXParameters;
        X509Certificate x509Certificate3 = x509Certificate;
        PublicKey publicKey2 = publicKey;
        int i3 = i;
        X509CRLStoreSelector x509CRLStoreSelector = new X509CRLStoreSelector();
        try {
            Iterator it;
            Iterator it2;
            int size;
            X509CRL x509crl;
            int i4;
            int i5;
            x509CRLStoreSelector.addIssuerName(CertPathValidatorUtilities.getEncodedIssuerPrincipal(x509Certificate).getEncoded());
            x509CRLStoreSelector.setCertificateChecking(x509Certificate3);
            int i6 = 0;
            try {
                Set findCRLs = CRL_UTIL.findCRLs(x509CRLStoreSelector, pKIXParameters2);
                it = findCRLs.iterator();
                if (findCRLs.isEmpty()) {
                    ArrayList arrayList = new ArrayList();
                    for (X509CRL issuerX500Principal : CRL_UTIL.findCRLs(new X509CRLStoreSelector(), pKIXParameters2)) {
                        arrayList.add(issuerX500Principal.getIssuerX500Principal());
                    }
                    size = arrayList.size();
                    addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noCrlInCertstore", new Object[]{new UntrustedInput(x509CRLStoreSelector.getIssuerNames()), new UntrustedInput(arrayList), Integers.valueOf(size)}), i3);
                }
            } catch (AnnotatedException e2) {
                addError(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlExtractionError", new Object[]{e2.getCause().getMessage(), e2.getCause(), e2.getCause().getClass().getName()}), i3);
                it = new ArrayList().iterator();
            }
            X509CRL x509crl2 = null;
            while (it.hasNext()) {
                x509crl2 = (X509CRL) it.next();
                if (x509crl2.getNextUpdate() == null || pKIXParameters.getDate().before(x509crl2.getNextUpdate())) {
                    addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.localValidCRL", new Object[]{new TrustedInput(x509crl2.getThisUpdate()), new TrustedInput(x509crl2.getNextUpdate())}), i3);
                    x509crl = x509crl2;
                    i4 = 1;
                    break;
                }
                addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.localInvalidCRL", new Object[]{new TrustedInput(x509crl2.getThisUpdate()), new TrustedInput(x509crl2.getNextUpdate())}), i3);
            }
            x509crl = x509crl2;
            i4 = 0;
            if (i4 == 0) {
                it2 = vector.iterator();
                i5 = i4;
                while (it2.hasNext()) {
                    try {
                        String str = (String) it2.next();
                        X509CRL crl = getCRL(str);
                        if (crl != null) {
                            if (x509Certificate.getIssuerX500Principal().equals(crl.getIssuerX500Principal())) {
                                if (crl.getNextUpdate() != null) {
                                    if (!this.pkixParams.getDate().before(crl.getNextUpdate())) {
                                        String str2 = RESOURCE_NAME;
                                        String str3 = "CertPathReviewer.onlineInvalidCRL";
                                        try {
                                            Object[] objArr = new Object[3];
                                            objArr[0] = new TrustedInput(crl.getThisUpdate());
                                            objArr[1] = new TrustedInput(crl.getNextUpdate());
                                            objArr[2] = new UntrustedUrlInput(str);
                                            addNotification(new ErrorBundle(str2, str3, objArr), i3);
                                        } catch (CertPathReviewerException e3) {
                                            e = e3;
                                            i2 = 3;
                                            addNotification(e.getErrorMessage(), i3);
                                            i6 = 0;
                                        }
                                    }
                                }
                                try {
                                    try {
                                        addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.onlineValidCRL", new Object[]{new TrustedInput(crl.getThisUpdate()), new TrustedInput(crl.getNextUpdate()), new UntrustedUrlInput(str)}), i3);
                                        x509crl = crl;
                                        i5 = 1;
                                        break;
                                    } catch (CertPathReviewerException e4) {
                                        e = e4;
                                    }
                                } catch (CertPathReviewerException e5) {
                                    e = e5;
                                    i5 = 1;
                                    addNotification(e.getErrorMessage(), i3);
                                    i6 = 0;
                                }
                            } else {
                                String str4 = RESOURCE_NAME;
                                String str5 = "CertPathReviewer.onlineCRLWrongCA";
                                try {
                                    Object[] objArr2 = new Object[3];
                                    objArr2[i6] = new UntrustedInput(crl.getIssuerX500Principal().getName());
                                    objArr2[1] = new UntrustedInput(x509Certificate.getIssuerX500Principal().getName());
                                    objArr2[2] = new UntrustedUrlInput(str);
                                    addNotification(new ErrorBundle(str4, str5, objArr2), i3);
                                } catch (CertPathReviewerException e6) {
                                    e = e6;
                                    i2 = 3;
                                    addNotification(e.getErrorMessage(), i3);
                                    i6 = 0;
                                }
                            }
                        }
                    } catch (CertPathReviewerException e7) {
                        e = e7;
                    }
                    i6 = 0;
                }
            } else {
                i5 = i4;
            }
            if (x509crl != null) {
                if (x509Certificate2 != null) {
                    boolean[] keyUsage = x509Certificate2.getKeyUsage();
                    if (keyUsage != null && (keyUsage.length < 7 || !keyUsage[6])) {
                        throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noCrlSigningPermited"));
                    }
                }
                if (publicKey2 != null) {
                    try {
                        int i7;
                        x509crl.verify(publicKey2, "BC");
                        X509CRLEntry revokedCertificate = x509crl.getRevokedCertificate(x509Certificate.getSerialNumber());
                        if (revokedCertificate != null) {
                            String str6;
                            LocaleString localeString;
                            if (revokedCertificate.hasExtensions()) {
                                try {
                                    ASN1Enumerated instance = ASN1Enumerated.getInstance(CertPathValidatorUtilities.getExtensionValue(revokedCertificate, Extension.reasonCode.getId()));
                                    if (instance != null) {
                                        str6 = crlReasons[instance.getValue().intValue()];
                                        if (str6 == null) {
                                            str6 = crlReasons[7];
                                        }
                                        localeString = new LocaleString(RESOURCE_NAME, str6);
                                        if (date.before(revokedCertificate.getRevocationDate())) {
                                            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.certRevoked", new Object[]{new TrustedInput(revokedCertificate.getRevocationDate()), localeString}));
                                        } else {
                                            addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.revokedAfterValidation", new Object[]{new TrustedInput(revokedCertificate.getRevocationDate()), localeString}), i3);
                                        }
                                    }
                                } catch (AnnotatedException e22) {
                                    throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlReasonExtError"), e22);
                                }
                            }
                            str6 = null;
                            if (str6 == null) {
                            }
                            localeString = new LocaleString(RESOURCE_NAME, str6);
                            if (date.before(revokedCertificate.getRevocationDate())) {
                            }
                        } else {
                            addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.notRevoked"), i3);
                        }
                        if (x509crl.getNextUpdate() == null || !x509crl.getNextUpdate().before(this.pkixParams.getDate())) {
                            size = 0;
                            i7 = 1;
                        } else {
                            i7 = 1;
                            Object[] objArr3 = new Object[1];
                            size = 0;
                            objArr3[0] = new TrustedInput(x509crl.getNextUpdate());
                            addNotification(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlUpdateAvailable", objArr3), i3);
                        }
                        try {
                            ASN1Primitive extensionValue = CertPathValidatorUtilities.getExtensionValue(x509crl, ISSUING_DISTRIBUTION_POINT);
                            try {
                                ASN1Primitive extensionValue2 = CertPathValidatorUtilities.getExtensionValue(x509crl, DELTA_CRL_INDICATOR);
                                if (extensionValue2 != null) {
                                    X509CRLStoreSelector x509CRLStoreSelector2 = new X509CRLStoreSelector();
                                    try {
                                        x509CRLStoreSelector2.addIssuerName(CertPathValidatorUtilities.getIssuerPrincipal(x509crl).getEncoded());
                                        x509CRLStoreSelector2.setMinCRLNumber(((ASN1Integer) extensionValue2).getPositiveValue());
                                        try {
                                            x509CRLStoreSelector2.setMaxCRLNumber(((ASN1Integer) CertPathValidatorUtilities.getExtensionValue(x509crl, CRL_NUMBER)).getPositiveValue().subtract(BigInteger.valueOf(1)));
                                            try {
                                                for (X509CRL extensionValue3 : CRL_UTIL.findCRLs(x509CRLStoreSelector2, pKIXParameters2)) {
                                                    try {
                                                        ASN1Primitive extensionValue4 = CertPathValidatorUtilities.getExtensionValue(extensionValue3, ISSUING_DISTRIBUTION_POINT);
                                                        if (extensionValue == null) {
                                                            if (extensionValue4 == null) {
                                                                break;
                                                            }
                                                        } else if (extensionValue.equals(extensionValue4)) {
                                                            break;
                                                        }
                                                    } catch (AnnotatedException e222) {
                                                        throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.distrPtExtError"), e222);
                                                    }
                                                }
                                                i7 = size;
                                                if (i7 == 0) {
                                                    throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noBaseCRL"));
                                                }
                                            } catch (AnnotatedException e2222) {
                                                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlExtractionError"), e2222);
                                            }
                                        } catch (AnnotatedException e22222) {
                                            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlNbrExtError"), e22222);
                                        }
                                    } catch (IOException e8) {
                                        throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlIssuerException"), e8);
                                    }
                                }
                                if (extensionValue != null) {
                                    IssuingDistributionPoint instance2 = IssuingDistributionPoint.getInstance(extensionValue);
                                    try {
                                        BasicConstraints instance3 = BasicConstraints.getInstance(CertPathValidatorUtilities.getExtensionValue(x509Certificate3, BASIC_CONSTRAINTS));
                                        if (instance2.onlyContainsUserCerts() && instance3 != null && instance3.isCA()) {
                                            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlOnlyUserCert"));
                                        } else if (instance2.onlyContainsCACerts() && (instance3 == null || !instance3.isCA())) {
                                            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlOnlyCaCert"));
                                        } else if (instance2.onlyContainsAttributeCerts()) {
                                            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlOnlyAttrCert"));
                                        }
                                    } catch (AnnotatedException e222222) {
                                        throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlBCExtError"), e222222);
                                    }
                                }
                            } catch (AnnotatedException e9) {
                                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.deltaCrlExtError"));
                            }
                        } catch (AnnotatedException e10) {
                            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.distrPtExtError"));
                        }
                    } catch (Exception e11) {
                        throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlVerifyFailed"), e11);
                    }
                }
                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlNoIssuerPublicKey"));
            }
            if (i5 == 0) {
                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.noValidCrlFound"));
            }
        } catch (IOException e82) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.crlIssuerException"), e82);
        }
    }

    protected void checkRevocation(PKIXParameters pKIXParameters, X509Certificate x509Certificate, Date date, X509Certificate x509Certificate2, PublicKey publicKey, Vector vector, Vector vector2, int i) throws CertPathReviewerException {
        checkCRLs(pKIXParameters, x509Certificate, date, x509Certificate2, publicKey, vector, i);
    }

    protected void doChecks() {
        if (!this.initialized) {
            throw new IllegalStateException("Object not initialized. Call init() first.");
        } else if (this.notifications == null) {
            this.notifications = new List[(this.n + 1)];
            this.errors = new List[(this.n + 1)];
            for (int i = 0; i < this.notifications.length; i++) {
                this.notifications[i] = new ArrayList();
                this.errors[i] = new ArrayList();
            }
            checkSignatures();
            checkNameConstraints();
            checkPathLength();
            checkPolicy();
            checkCriticalExtensions();
        }
    }

    protected Vector getCRLDistUrls(CRLDistPoint cRLDistPoint) {
        Vector vector = new Vector();
        if (cRLDistPoint != null) {
            DistributionPoint[] distributionPoints = cRLDistPoint.getDistributionPoints();
            for (DistributionPoint distributionPoint : distributionPoints) {
                DistributionPointName distributionPoint2 = distributionPoint.getDistributionPoint();
                if (distributionPoint2.getType() == 0) {
                    GeneralName[] names = GeneralNames.getInstance(distributionPoint2.getName()).getNames();
                    for (int i = 0; i < names.length; i++) {
                        if (names[i].getTagNo() == 6) {
                            vector.add(((DERIA5String) names[i].getName()).getString());
                        }
                    }
                }
            }
        }
        return vector;
    }

    public CertPath getCertPath() {
        return this.certPath;
    }

    public int getCertPathSize() {
        return this.n;
    }

    public List getErrors(int i) {
        doChecks();
        return this.errors[i + 1];
    }

    public List[] getErrors() {
        doChecks();
        return this.errors;
    }

    public List getNotifications(int i) {
        doChecks();
        return this.notifications[i + 1];
    }

    public List[] getNotifications() {
        doChecks();
        return this.notifications;
    }

    protected Vector getOCSPUrls(AuthorityInformationAccess authorityInformationAccess) {
        Vector vector = new Vector();
        if (authorityInformationAccess != null) {
            AccessDescription[] accessDescriptions = authorityInformationAccess.getAccessDescriptions();
            for (int i = 0; i < accessDescriptions.length; i++) {
                if (accessDescriptions[i].getAccessMethod().equals(AccessDescription.id_ad_ocsp)) {
                    GeneralName accessLocation = accessDescriptions[i].getAccessLocation();
                    if (accessLocation.getTagNo() == 6) {
                        vector.add(((DERIA5String) accessLocation.getName()).getString());
                    }
                }
            }
        }
        return vector;
    }

    public PolicyNode getPolicyTree() {
        doChecks();
        return this.policyTree;
    }

    public PublicKey getSubjectPublicKey() {
        doChecks();
        return this.subjectPublicKey;
    }

    public TrustAnchor getTrustAnchor() {
        doChecks();
        return this.trustAnchor;
    }

    protected Collection getTrustAnchors(X509Certificate x509Certificate, Set set) throws CertPathReviewerException {
        ArrayList arrayList = new ArrayList();
        X509CertSelector x509CertSelector = new X509CertSelector();
        try {
            x509CertSelector.setSubject(CertPathValidatorUtilities.getEncodedIssuerPrincipal(x509Certificate).getEncoded());
            byte[] extensionValue = x509Certificate.getExtensionValue(Extension.authorityKeyIdentifier.getId());
            if (extensionValue != null) {
                AuthorityKeyIdentifier instance = AuthorityKeyIdentifier.getInstance(ASN1Primitive.fromByteArray(((ASN1OctetString) ASN1Primitive.fromByteArray(extensionValue)).getOctets()));
                x509CertSelector.setSerialNumber(instance.getAuthorityCertSerialNumber());
                extensionValue = instance.getKeyIdentifier();
                if (extensionValue != null) {
                    x509CertSelector.setSubjectKeyIdentifier(new DEROctetString(extensionValue).getEncoded());
                }
            }
            for (TrustAnchor trustAnchor : set) {
                if (trustAnchor.getTrustedCert() != null) {
                    if (!x509CertSelector.match(trustAnchor.getTrustedCert())) {
                    }
                } else if (trustAnchor.getCAName() != null) {
                    if (trustAnchor.getCAPublicKey() != null) {
                        if (!CertPathValidatorUtilities.getEncodedIssuerPrincipal(x509Certificate).equals(new X500Principal(trustAnchor.getCAName()))) {
                        }
                    }
                }
                arrayList.add(trustAnchor);
            }
            return arrayList;
        } catch (IOException e) {
            throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.trustAnchorIssuerError"));
        }
    }

    public void init(CertPath certPath, PKIXParameters pKIXParameters) throws CertPathReviewerException {
        if (this.initialized) {
            throw new IllegalStateException("object is already initialized!");
        }
        this.initialized = true;
        if (certPath != null) {
            this.certPath = certPath;
            this.certs = certPath.getCertificates();
            this.n = this.certs.size();
            if (this.certs.isEmpty()) {
                throw new CertPathReviewerException(new ErrorBundle(RESOURCE_NAME, "CertPathReviewer.emptyCertPath"));
            }
            this.pkixParams = (PKIXParameters) pKIXParameters.clone();
            this.validDate = CertPathValidatorUtilities.getValidDate(this.pkixParams);
            this.notifications = null;
            this.errors = null;
            this.trustAnchor = null;
            this.subjectPublicKey = null;
            this.policyTree = null;
            return;
        }
        throw new NullPointerException("certPath was null");
    }

    public boolean isValidCertPath() {
        doChecks();
        for (List isEmpty : this.errors) {
            if (!isEmpty.isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
