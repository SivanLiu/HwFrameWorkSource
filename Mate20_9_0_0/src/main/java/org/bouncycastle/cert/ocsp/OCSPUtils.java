package org.bouncycastle.cert.ocsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.x509.Extensions;
import org.bouncycastle.cert.X509CertificateHolder;

class OCSPUtils {
    static final X509CertificateHolder[] EMPTY_CERTS = new X509CertificateHolder[0];
    static List EMPTY_LIST = Collections.unmodifiableList(new ArrayList());
    static Set EMPTY_SET = Collections.unmodifiableSet(new HashSet());

    OCSPUtils() {
    }

    static Date extractDate(ASN1GeneralizedTime aSN1GeneralizedTime) {
        try {
            return aSN1GeneralizedTime.getDate();
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception processing GeneralizedTime: ");
            stringBuilder.append(e.getMessage());
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    static Set getCriticalExtensionOIDs(Extensions extensions) {
        return extensions == null ? EMPTY_SET : Collections.unmodifiableSet(new HashSet(Arrays.asList(extensions.getCriticalExtensionOIDs())));
    }

    static List getExtensionOIDs(Extensions extensions) {
        return extensions == null ? EMPTY_LIST : Collections.unmodifiableList(Arrays.asList(extensions.getExtensionOIDs()));
    }

    static Set getNonCriticalExtensionOIDs(Extensions extensions) {
        return extensions == null ? EMPTY_SET : Collections.unmodifiableSet(new HashSet(Arrays.asList(extensions.getNonCriticalExtensionOIDs())));
    }
}
