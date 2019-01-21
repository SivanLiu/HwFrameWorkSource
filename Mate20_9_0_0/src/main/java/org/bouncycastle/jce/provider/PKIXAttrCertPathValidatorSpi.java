package org.bouncycastle.jce.provider;

import java.security.InvalidAlgorithmParameterException;
import java.security.cert.CertPath;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;
import java.security.cert.CertPathValidatorSpi;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Set;
import org.bouncycastle.jcajce.PKIXCertStoreSelector;
import org.bouncycastle.jcajce.PKIXExtendedParameters;
import org.bouncycastle.jcajce.PKIXExtendedParameters.Builder;
import org.bouncycastle.jcajce.util.BCJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jce.exception.ExtCertPathValidatorException;
import org.bouncycastle.x509.ExtendedPKIXParameters;
import org.bouncycastle.x509.X509AttributeCertStoreSelector;
import org.bouncycastle.x509.X509AttributeCertificate;

public class PKIXAttrCertPathValidatorSpi extends CertPathValidatorSpi {
    private final JcaJceHelper helper = new BCJcaJceHelper();

    public CertPathValidatorResult engineValidate(CertPath certPath, CertPathParameters certPathParameters) throws CertPathValidatorException, InvalidAlgorithmParameterException {
        boolean z = certPathParameters instanceof ExtendedPKIXParameters;
        StringBuilder stringBuilder;
        if (z || (certPathParameters instanceof PKIXExtendedParameters)) {
            PKIXExtendedParameters build;
            Set hashSet = new HashSet();
            Set hashSet2 = new HashSet();
            Set hashSet3 = new HashSet();
            HashSet hashSet4 = new HashSet();
            if (certPathParameters instanceof PKIXParameters) {
                Builder builder = new Builder((PKIXParameters) certPathParameters);
                if (z) {
                    ExtendedPKIXParameters extendedPKIXParameters = (ExtendedPKIXParameters) certPathParameters;
                    builder.setUseDeltasEnabled(extendedPKIXParameters.isUseDeltasEnabled());
                    builder.setValidityModel(extendedPKIXParameters.getValidityModel());
                    Set attrCertCheckers = extendedPKIXParameters.getAttrCertCheckers();
                    hashSet = extendedPKIXParameters.getProhibitedACAttributes();
                    hashSet3 = extendedPKIXParameters.getNecessaryACAttributes();
                    hashSet2 = hashSet;
                    hashSet = attrCertCheckers;
                }
                build = builder.build();
            } else {
                build = (PKIXExtendedParameters) certPathParameters;
            }
            PKIXExtendedParameters pKIXExtendedParameters = build;
            PKIXCertStoreSelector targetConstraints = pKIXExtendedParameters.getTargetConstraints();
            if (targetConstraints instanceof X509AttributeCertStoreSelector) {
                X509AttributeCertificate attributeCert = ((X509AttributeCertStoreSelector) targetConstraints).getAttributeCert();
                CertPath processAttrCert1 = RFC3281CertPathUtilities.processAttrCert1(attributeCert, pKIXExtendedParameters);
                CertPathValidatorResult processAttrCert2 = RFC3281CertPathUtilities.processAttrCert2(certPath, pKIXExtendedParameters);
                X509Certificate x509Certificate = (X509Certificate) certPath.getCertificates().get(0);
                RFC3281CertPathUtilities.processAttrCert3(x509Certificate, pKIXExtendedParameters);
                RFC3281CertPathUtilities.processAttrCert4(x509Certificate, hashSet4);
                RFC3281CertPathUtilities.processAttrCert5(attributeCert, pKIXExtendedParameters);
                RFC3281CertPathUtilities.processAttrCert7(attributeCert, certPath, processAttrCert1, pKIXExtendedParameters, hashSet);
                RFC3281CertPathUtilities.additionalChecks(attributeCert, hashSet2, hashSet3);
                try {
                    RFC3281CertPathUtilities.checkCRLs(attributeCert, pKIXExtendedParameters, x509Certificate, CertPathValidatorUtilities.getValidCertDateFromValidityModel(pKIXExtendedParameters, null, -1), certPath.getCertificates(), this.helper);
                    return processAttrCert2;
                } catch (AnnotatedException e) {
                    throw new ExtCertPathValidatorException("Could not get validity date from attribute certificate.", e);
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("TargetConstraints must be an instance of ");
            stringBuilder.append(X509AttributeCertStoreSelector.class.getName());
            stringBuilder.append(" for ");
            stringBuilder.append(getClass().getName());
            stringBuilder.append(" class.");
            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("Parameters must be a ");
        stringBuilder.append(ExtendedPKIXParameters.class.getName());
        stringBuilder.append(" instance.");
        throw new InvalidAlgorithmParameterException(stringBuilder.toString());
    }
}
