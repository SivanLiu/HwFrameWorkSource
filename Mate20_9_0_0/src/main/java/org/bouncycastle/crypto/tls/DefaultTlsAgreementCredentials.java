package org.bouncycastle.crypto.tls;

import java.math.BigInteger;
import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.agreement.DHBasicAgreement;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.util.BigIntegers;

public class DefaultTlsAgreementCredentials extends AbstractTlsAgreementCredentials {
    protected BasicAgreement basicAgreement;
    protected Certificate certificate;
    protected AsymmetricKeyParameter privateKey;
    protected boolean truncateAgreement;

    public DefaultTlsAgreementCredentials(Certificate certificate, AsymmetricKeyParameter asymmetricKeyParameter) {
        if (certificate == null) {
            throw new IllegalArgumentException("'certificate' cannot be null");
        } else if (certificate.isEmpty()) {
            throw new IllegalArgumentException("'certificate' cannot be empty");
        } else if (asymmetricKeyParameter == null) {
            throw new IllegalArgumentException("'privateKey' cannot be null");
        } else if (asymmetricKeyParameter.isPrivate()) {
            boolean z;
            if (asymmetricKeyParameter instanceof DHPrivateKeyParameters) {
                this.basicAgreement = new DHBasicAgreement();
                z = true;
            } else if (asymmetricKeyParameter instanceof ECPrivateKeyParameters) {
                this.basicAgreement = new ECDHBasicAgreement();
                z = false;
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("'privateKey' type not supported: ");
                stringBuilder.append(asymmetricKeyParameter.getClass().getName());
                throw new IllegalArgumentException(stringBuilder.toString());
            }
            this.truncateAgreement = z;
            this.certificate = certificate;
            this.privateKey = asymmetricKeyParameter;
        } else {
            throw new IllegalArgumentException("'privateKey' must be private");
        }
    }

    public byte[] generateAgreement(AsymmetricKeyParameter asymmetricKeyParameter) {
        this.basicAgreement.init(this.privateKey);
        BigInteger calculateAgreement = this.basicAgreement.calculateAgreement(asymmetricKeyParameter);
        return this.truncateAgreement ? BigIntegers.asUnsignedByteArray(calculateAgreement) : BigIntegers.asUnsignedByteArray(this.basicAgreement.getFieldSize(), calculateAgreement);
    }

    public Certificate getCertificate() {
        return this.certificate;
    }
}
