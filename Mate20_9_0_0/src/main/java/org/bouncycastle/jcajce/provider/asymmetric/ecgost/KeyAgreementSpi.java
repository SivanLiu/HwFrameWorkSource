package org.bouncycastle.jcajce.provider.asymmetric.ecgost;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.DerivationFunction;
import org.bouncycastle.crypto.agreement.ECVKOAgreement;
import org.bouncycastle.crypto.digests.GOST3411Digest;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithUKM;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.jcajce.provider.asymmetric.util.BaseAgreementSpi;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jcajce.spec.UserKeyingMaterialSpec;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;

public class KeyAgreementSpi extends BaseAgreementSpi {
    private static final X9IntegerConverter converter = new X9IntegerConverter();
    private ECVKOAgreement agreement;
    private String kaAlgorithm;
    private ECDomainParameters parameters;
    private byte[] result;

    public static class ECVKO extends KeyAgreementSpi {
        public ECVKO() {
            super("ECGOST3410", new ECVKOAgreement(new GOST3411Digest()), null);
        }
    }

    protected KeyAgreementSpi(String str, ECVKOAgreement eCVKOAgreement, DerivationFunction derivationFunction) {
        super(str, derivationFunction);
        this.kaAlgorithm = str;
        this.agreement = eCVKOAgreement;
    }

    static AsymmetricKeyParameter generatePublicKeyParameter(PublicKey publicKey) throws InvalidKeyException {
        return publicKey instanceof BCECPublicKey ? ((BCECGOST3410PublicKey) publicKey).engineGetKeyParameters() : ECUtil.generatePublicKeyParameter(publicKey);
    }

    private static String getSimpleName(Class cls) {
        String name = cls.getName();
        return name.substring(name.lastIndexOf(46) + 1);
    }

    private void initFromKey(Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException {
        if (key instanceof PrivateKey) {
            ECPrivateKeyParameters eCPrivateKeyParameters = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter((PrivateKey) key);
            this.parameters = eCPrivateKeyParameters.getParameters();
            this.ukmParameters = algorithmParameterSpec instanceof UserKeyingMaterialSpec ? ((UserKeyingMaterialSpec) algorithmParameterSpec).getUserKeyingMaterial() : null;
            this.agreement.init(new ParametersWithUKM(eCPrivateKeyParameters, this.ukmParameters));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.kaAlgorithm);
        stringBuilder.append(" key agreement requires ");
        stringBuilder.append(getSimpleName(ECPrivateKey.class));
        stringBuilder.append(" for initialisation");
        throw new InvalidKeyException(stringBuilder.toString());
    }

    protected byte[] calcSecret() {
        return this.result;
    }

    protected Key engineDoPhase(Key key, boolean z) throws InvalidKeyException, IllegalStateException {
        StringBuilder stringBuilder;
        if (this.parameters == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.kaAlgorithm);
            stringBuilder.append(" not initialised.");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (!z) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.kaAlgorithm);
            stringBuilder.append(" can only be between two parties.");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (key instanceof PublicKey) {
            try {
                this.result = this.agreement.calculateAgreement(generatePublicKeyParameter((PublicKey) key));
                return null;
            } catch (final Exception e) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("calculation failed: ");
                stringBuilder2.append(e.getMessage());
                throw new InvalidKeyException(stringBuilder2.toString()) {
                    public Throwable getCause() {
                        return e;
                    }
                };
            }
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.kaAlgorithm);
            stringBuilder.append(" key agreement requires ");
            stringBuilder.append(getSimpleName(ECPublicKey.class));
            stringBuilder.append(" for doPhase");
            throw new InvalidKeyException(stringBuilder.toString());
        }
    }

    protected void engineInit(Key key, SecureRandom secureRandom) throws InvalidKeyException {
        initFromKey(key, null);
    }

    protected void engineInit(Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (algorithmParameterSpec == null || (algorithmParameterSpec instanceof UserKeyingMaterialSpec)) {
            initFromKey(key, algorithmParameterSpec);
            return;
        }
        throw new InvalidAlgorithmParameterException("No algorithm parameters supported");
    }
}
