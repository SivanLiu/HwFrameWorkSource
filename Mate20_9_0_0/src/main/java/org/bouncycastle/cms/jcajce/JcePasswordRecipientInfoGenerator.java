package org.bouncycastle.cms.jcajce;

import java.security.Key;
import java.security.Provider;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.PasswordRecipientInfoGenerator;
import org.bouncycastle.operator.GenericKey;

public class JcePasswordRecipientInfoGenerator extends PasswordRecipientInfoGenerator {
    private EnvelopedDataHelper helper = new EnvelopedDataHelper(new DefaultJcaJceExtHelper());

    public JcePasswordRecipientInfoGenerator(ASN1ObjectIdentifier aSN1ObjectIdentifier, char[] cArr) {
        super(aSN1ObjectIdentifier, cArr);
    }

    protected byte[] calculateDerivedKey(int i, AlgorithmIdentifier algorithmIdentifier, int i2) throws CMSException {
        return this.helper.calculateDerivedKey(i, this.password, algorithmIdentifier, i2);
    }

    public byte[] generateEncryptedBytes(AlgorithmIdentifier algorithmIdentifier, byte[] bArr, GenericKey genericKey) throws CMSException {
        Key jceKey = this.helper.getJceKey(genericKey);
        Cipher createRFC3211Wrapper = this.helper.createRFC3211Wrapper(algorithmIdentifier.getAlgorithm());
        try {
            createRFC3211Wrapper.init(3, new SecretKeySpec(bArr, createRFC3211Wrapper.getAlgorithm()), new IvParameterSpec(ASN1OctetString.getInstance(algorithmIdentifier.getParameters()).getOctets()));
            return createRFC3211Wrapper.wrap(jceKey);
        } catch (Exception e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("cannot process content encryption key: ");
            stringBuilder.append(e.getMessage());
            throw new CMSException(stringBuilder.toString(), e);
        }
    }

    public JcePasswordRecipientInfoGenerator setProvider(String str) {
        this.helper = new EnvelopedDataHelper(new NamedJcaJceExtHelper(str));
        return this;
    }

    public JcePasswordRecipientInfoGenerator setProvider(Provider provider) {
        this.helper = new EnvelopedDataHelper(new ProviderJcaJceExtHelper(provider));
        return this;
    }
}
