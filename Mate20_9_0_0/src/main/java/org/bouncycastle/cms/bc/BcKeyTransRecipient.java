package org.bouncycastle.cms.bc;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.KeyTransRecipient;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.operator.bc.BcRSAAsymmetricKeyUnwrapper;

public abstract class BcKeyTransRecipient implements KeyTransRecipient {
    private AsymmetricKeyParameter recipientKey;

    public BcKeyTransRecipient(AsymmetricKeyParameter asymmetricKeyParameter) {
        this.recipientKey = asymmetricKeyParameter;
    }

    protected CipherParameters extractSecretKey(AlgorithmIdentifier algorithmIdentifier, AlgorithmIdentifier algorithmIdentifier2, byte[] bArr) throws CMSException {
        try {
            return CMSUtils.getBcKey(new BcRSAAsymmetricKeyUnwrapper(algorithmIdentifier, this.recipientKey).generateUnwrappedKey(algorithmIdentifier2, bArr));
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception unwrapping key: ");
            stringBuilder.append(e.getMessage());
            throw new CMSException(stringBuilder.toString(), e);
        }
    }
}
