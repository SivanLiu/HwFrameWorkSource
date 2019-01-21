package sun.security.provider.certpath;

import java.security.cert.CertPath;
import java.security.cert.CertPathValidatorException;
import java.security.cert.PKIXCertPathChecker;
import java.security.cert.PKIXReason;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import sun.security.util.Debug;

class PKIXMasterCertPathValidator {
    private static final Debug debug = Debug.getInstance("certpath");

    PKIXMasterCertPathValidator() {
    }

    static void validate(CertPath cpOriginal, List<X509Certificate> reversedCertList, List<PKIXCertPathChecker> certPathCheckers) throws CertPathValidatorException {
        List<PKIXCertPathChecker> list;
        int cpSize = reversedCertList.size();
        if (debug != null) {
            debug.println("--------------------------------------------------------------");
            debug.println("Executing PKIX certification path validation algorithm.");
        }
        int i = 0;
        while (i < cpSize) {
            StringBuilder stringBuilder;
            X509Certificate currCert = (X509Certificate) reversedCertList.get(i);
            if (debug != null) {
                Debug debug = debug;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Checking cert");
                stringBuilder2.append(i + 1);
                stringBuilder2.append(" - Subject: ");
                stringBuilder2.append(currCert.getSubjectX500Principal());
                debug.println(stringBuilder2.toString());
            }
            Set<String> unresCritExts = currCert.getCriticalExtensionOIDs();
            if (unresCritExts == null) {
                unresCritExts = Collections.emptySet();
            }
            if (!(debug == null || unresCritExts.isEmpty())) {
                StringJoiner joiner = new StringJoiner(", ", "{", "}");
                for (String oid : unresCritExts) {
                    joiner.add(oid);
                }
                Debug debug2 = debug;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Set of critical extensions: ");
                stringBuilder.append(joiner.toString());
                debug2.println(stringBuilder.toString());
            }
            int j = 0;
            while (j < certPathCheckers.size()) {
                Debug debug3;
                StringBuilder stringBuilder3;
                PKIXCertPathChecker currChecker = (PKIXCertPathChecker) certPathCheckers.get(j);
                if (debug != null) {
                    debug3 = debug;
                    stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("-Using checker");
                    stringBuilder3.append(j + 1);
                    stringBuilder3.append(" ... [");
                    stringBuilder3.append(currChecker.getClass().getName());
                    stringBuilder3.append("]");
                    debug3.println(stringBuilder3.toString());
                }
                if (i == 0) {
                    currChecker.init(false);
                }
                try {
                    currChecker.check(currCert, unresCritExts);
                    if (debug != null) {
                        debug3 = debug;
                        stringBuilder3 = new StringBuilder();
                        stringBuilder3.append("-checker");
                        stringBuilder3.append(j + 1);
                        stringBuilder3.append(" validation succeeded");
                        debug3.println(stringBuilder3.toString());
                    }
                    j++;
                } catch (CertPathValidatorException cpve) {
                    throw new CertPathValidatorException(cpve.getMessage(), cpve.getCause() != null ? cpve.getCause() : cpve, cpOriginal, cpSize - (i + 1), cpve.getReason());
                }
            }
            list = certPathCheckers;
            if (unresCritExts.isEmpty()) {
                if (debug != null) {
                    Debug debug4 = debug;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("\ncert");
                    stringBuilder.append(i + 1);
                    stringBuilder.append(" validation succeeded.\n");
                    debug4.println(stringBuilder.toString());
                }
                i++;
            } else {
                throw new CertPathValidatorException("unrecognized critical extension(s)", null, cpOriginal, cpSize - (i + 1), PKIXReason.UNRECOGNIZED_CRIT_EXT);
            }
        }
        List<X509Certificate> list2 = reversedCertList;
        list = certPathCheckers;
        if (debug != null) {
            debug.println("Cert path validation succeeded. (PKIX validation algorithm)");
            debug.println("--------------------------------------------------------------");
        }
    }
}
