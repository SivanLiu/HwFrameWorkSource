package org.bouncycastle.jce.provider;

import java.security.cert.CertStore;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bouncycastle.jcajce.PKIXCRLStoreSelector;
import org.bouncycastle.util.Store;

class PKIXCRLUtil {
    PKIXCRLUtil() {
    }

    private final Collection findCRLs(PKIXCRLStoreSelector pKIXCRLStoreSelector, List list) throws AnnotatedException {
        AnnotatedException annotatedException;
        Collection hashSet = new HashSet();
        AnnotatedException annotatedException2 = null;
        Object obj = null;
        for (Object next : list) {
            if (next instanceof Store) {
                try {
                    hashSet.addAll(((Store) next).getMatches(pKIXCRLStoreSelector));
                } catch (Throwable e) {
                    annotatedException = new AnnotatedException("Exception searching in X.509 CRL store.", e);
                    annotatedException2 = annotatedException;
                }
            } else {
                try {
                    hashSet.addAll(PKIXCRLStoreSelector.getCRLs(pKIXCRLStoreSelector, (CertStore) next));
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

    public Set findCRLs(PKIXCRLStoreSelector pKIXCRLStoreSelector, Date date, List list, List list2) throws AnnotatedException {
        Set<X509CRL> hashSet = new HashSet();
        try {
            hashSet.addAll(findCRLs(pKIXCRLStoreSelector, list2));
            hashSet.addAll(findCRLs(pKIXCRLStoreSelector, list));
            Set hashSet2 = new HashSet();
            for (X509CRL x509crl : hashSet) {
                if (x509crl.getNextUpdate().after(date)) {
                    X509Certificate certificateChecking = pKIXCRLStoreSelector.getCertificateChecking();
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
