package org.bouncycastle.operator.jcajce;

import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.Provider;
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper;
import org.bouncycastle.operator.AsymmetricKeyUnwrapper;
import org.bouncycastle.operator.GenericKey;
import org.bouncycastle.operator.OperatorException;

public class JceAsymmetricKeyUnwrapper extends AsymmetricKeyUnwrapper {
    private Map extraMappings = new HashMap();
    private OperatorHelper helper = new OperatorHelper(new DefaultJcaJceHelper());
    private PrivateKey privKey;
    private boolean unwrappedKeyMustBeEncodable;

    public JceAsymmetricKeyUnwrapper(AlgorithmIdentifier algorithmIdentifier, PrivateKey privateKey) {
        super(algorithmIdentifier);
        this.privKey = privateKey;
    }

    /* JADX WARNING: Removed duplicated region for block: B:20:0x004e  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0048 A:{Splitter: B:4:0x001e, ExcHandler: java.security.GeneralSecurityException (e java.security.GeneralSecurityException)} */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0048 A:{Splitter: B:4:0x001e, ExcHandler: java.security.GeneralSecurityException (e java.security.GeneralSecurityException)} */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0048 A:{Splitter: B:4:0x001e, ExcHandler: java.security.GeneralSecurityException (e java.security.GeneralSecurityException)} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x004a A:{Splitter: B:8:0x0038, ExcHandler: java.security.GeneralSecurityException (e java.security.GeneralSecurityException)} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x004a A:{Splitter: B:8:0x0038, ExcHandler: java.security.GeneralSecurityException (e java.security.GeneralSecurityException)} */
    /* JADX WARNING: Removed duplicated region for block: B:18:0x004a A:{Splitter: B:8:0x0038, ExcHandler: java.security.GeneralSecurityException (e java.security.GeneralSecurityException)} */
    /* JADX WARNING: Missing block: B:15:0x0043, code:
            if (r2.length != 0) goto L_0x0046;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public GenericKey generateUnwrappedKey(AlgorithmIdentifier algorithmIdentifier, byte[] bArr) throws OperatorException {
        StringBuilder stringBuilder;
        try {
            Cipher createAsymmetricWrapper = this.helper.createAsymmetricWrapper(getAlgorithmIdentifier().getAlgorithm(), this.extraMappings);
            AlgorithmParameters createAlgorithmParameters = this.helper.createAlgorithmParameters(getAlgorithmIdentifier());
            Key key = null;
            if (createAlgorithmParameters != null) {
                try {
                    createAsymmetricWrapper.init(4, this.privKey, createAlgorithmParameters);
                } catch (GeneralSecurityException e) {
                }
            } else {
                createAsymmetricWrapper.init(4, this.privKey);
            }
            Key unwrap = createAsymmetricWrapper.unwrap(bArr, this.helper.getKeyAlgorithmName(algorithmIdentifier.getAlgorithm()), 3);
            try {
                if (this.unwrappedKeyMustBeEncodable) {
                    byte[] encoded = unwrap.getEncoded();
                    if (encoded != null) {
                    }
                    if (key == null) {
                        createAsymmetricWrapper.init(2, this.privKey);
                        key = new SecretKeySpec(createAsymmetricWrapper.doFinal(bArr), algorithmIdentifier.getAlgorithm().getId());
                    }
                    return new JceGenericKey(algorithmIdentifier, key);
                }
            } catch (GeneralSecurityException e2) {
            }
            key = unwrap;
            if (key == null) {
            }
            return new JceGenericKey(algorithmIdentifier, key);
        } catch (Throwable e3) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("key invalid: ");
            stringBuilder.append(e3.getMessage());
            throw new OperatorException(stringBuilder.toString(), e3);
        } catch (Throwable e32) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("illegal blocksize: ");
            stringBuilder.append(e32.getMessage());
            throw new OperatorException(stringBuilder.toString(), e32);
        } catch (Throwable e322) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("bad padding: ");
            stringBuilder.append(e322.getMessage());
            throw new OperatorException(stringBuilder.toString(), e322);
        }
    }

    public JceAsymmetricKeyUnwrapper setAlgorithmMapping(ASN1ObjectIdentifier aSN1ObjectIdentifier, String str) {
        this.extraMappings.put(aSN1ObjectIdentifier, str);
        return this;
    }

    public JceAsymmetricKeyUnwrapper setMustProduceEncodableUnwrappedKey(boolean z) {
        this.unwrappedKeyMustBeEncodable = z;
        return this;
    }

    public JceAsymmetricKeyUnwrapper setProvider(String str) {
        this.helper = new OperatorHelper(new NamedJcaJceHelper(str));
        return this;
    }

    public JceAsymmetricKeyUnwrapper setProvider(Provider provider) {
        this.helper = new OperatorHelper(new ProviderJcaJceHelper(provider));
        return this;
    }
}
