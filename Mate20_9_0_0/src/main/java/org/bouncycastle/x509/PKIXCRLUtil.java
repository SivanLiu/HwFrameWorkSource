package org.bouncycastle.x509;

import java.security.cert.CertStore;
import java.security.cert.PKIXParameters;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bouncycastle.jce.provider.AnnotatedException;

class PKIXCRLUtil {
    PKIXCRLUtil() {
    }

    private final Collection findCRLs(X509CRLStoreSelector x509CRLStoreSelector, List list) throws AnnotatedException {
        AnnotatedException annotatedException;
        Collection hashSet = new HashSet();
        AnnotatedException annotatedException2 = null;
        Object obj = null;
        for (Object next : list) {
            if (next instanceof X509Store) {
                try {
                    hashSet.addAll(((X509Store) next).getMatches(x509CRLStoreSelector));
                } catch (Throwable e) {
                    annotatedException = new AnnotatedException("Exception searching in X.509 CRL store.", e);
                    annotatedException2 = annotatedException;
                }
            } else {
                try {
                    hashSet.addAll(((CertStore) next).getCRLs(x509CRLStoreSelector));
                } catch (Throwable e2) {
                    annotatedException = new AnnotatedException("Exception searching in X.509 CRL store.", e2);
                }
            }
            obj = 1;
        }
        if (obj != null || annotatedException2 == null) {
            return hashSet;
        }
        throw annotatedException2;
    }

    public Set findCRLs(X509CRLStoreSelector x509CRLStoreSelector, PKIXParameters pKIXParameters) throws AnnotatedException {
        Set hashSet = new HashSet();
        try {
            hashSet.addAll(findCRLs(x509CRLStoreSelector, pKIXParameters.getCertStores()));
            return hashSet;
        } catch (Throwable e) {
            throw new AnnotatedException("Exception obtaining complete CRLs.", e);
        }
    }

    public Set findCRLs(X509CRLStoreSelector x509CRLStoreSelector, ExtendedPKIXParameters extendedPKIXParameters, Date date) throws AnnotatedException {
        Set<X509CRL> hashSet = new HashSet();
        try {
            hashSet.addAll(findCRLs(x509CRLStoreSelector, extendedPKIXParameters.getAdditionalStores()));
            hashSet.addAll(findCRLs(x509CRLStoreSelector, extendedPKIXParameters.getStores()));
            hashSet.addAll(findCRLs(x509CRLStoreSelector, extendedPKIXParameters.getCertStores()));
            Set hashSet2 = new HashSet();
            if (extendedPKIXParameters.getDate() != null) {
                date = extendedPKIXParameters.getDate();
            }
            for (X509CRL x509crl : hashSet) {
                if (x509crl.getNextUpdate().after(date)) {
                    X509Certificate certificateChecking = x509CRLStoreSelector.getCertificateChecking();
                    if (certificateChecking == null || x509crl.getThisUpdate().before(certificateChecking.getNotAfter())) {
                        hashSet2.add(x509crl);
                    }
                }
            }
            return hashSet2;
        } catch (Throwable e) {
            throw new AnnotatedException("Exception obtaining complete CRLs.", e);
        }
    }
}
