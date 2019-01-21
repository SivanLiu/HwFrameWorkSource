package com.android.org.bouncycastle.jcajce.provider.asymmetric.ec;

import com.android.org.bouncycastle.asn1.x9.X9IntegerConverter;
import com.android.org.bouncycastle.crypto.BasicAgreement;
import com.android.org.bouncycastle.crypto.DerivationFunction;
import com.android.org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import com.android.org.bouncycastle.crypto.params.ECDomainParameters;
import com.android.org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.BaseAgreementSpi;
import com.android.org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import com.android.org.bouncycastle.jcajce.spec.UserKeyingMaterialSpec;
import com.android.org.bouncycastle.jce.interfaces.ECPrivateKey;
import com.android.org.bouncycastle.jce.interfaces.ECPublicKey;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

public class KeyAgreementSpi extends BaseAgreementSpi {
    private static final X9IntegerConverter converter = new X9IntegerConverter();
    private BasicAgreement agreement;
    private String kaAlgorithm;
    private ECDomainParameters parameters;
    private BigInteger result;

    public static class DH extends KeyAgreementSpi {
        public DH() {
            super("ECDH", new ECDHBasicAgreement(), null);
        }
    }

    protected KeyAgreementSpi(String kaAlgorithm, BasicAgreement agreement, DerivationFunction kdf) {
        super(kaAlgorithm, kdf);
        this.kaAlgorithm = kaAlgorithm;
        this.agreement = agreement;
    }

    protected byte[] bigIntToBytes(BigInteger r) {
        return converter.integerToBytes(r, converter.getByteLength(this.parameters.getCurve()));
    }

    protected Key engineDoPhase(Key key, boolean lastPhase) throws InvalidKeyException, IllegalStateException {
        StringBuilder stringBuilder;
        if (this.parameters == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.kaAlgorithm);
            stringBuilder.append(" not initialised.");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (!lastPhase) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.kaAlgorithm);
            stringBuilder.append(" can only be between two parties.");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (key instanceof PublicKey) {
            try {
                this.result = this.agreement.calculateAgreement(ECUtils.generatePublicKeyParameter((PublicKey) key));
                return null;
            } catch (Exception e) {
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

    protected void engineInit(Key key, AlgorithmParameterSpec params, SecureRandom random) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (params == null || (params instanceof UserKeyingMaterialSpec)) {
            initFromKey(key, params);
            return;
        }
        throw new InvalidAlgorithmParameterException("No algorithm parameters supported");
    }

    protected void engineInit(Key key, SecureRandom random) throws InvalidKeyException {
        initFromKey(key, null);
    }

    private void initFromKey(Key key, AlgorithmParameterSpec parameterSpec) throws InvalidKeyException {
        if (key instanceof PrivateKey) {
            ECPrivateKeyParameters privKey = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter((PrivateKey) key);
            this.parameters = privKey.getParameters();
            this.ukmParameters = parameterSpec instanceof UserKeyingMaterialSpec ? ((UserKeyingMaterialSpec) parameterSpec).getUserKeyingMaterial() : null;
            this.agreement.init(privKey);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.kaAlgorithm);
        stringBuilder.append(" key agreement requires ");
        stringBuilder.append(getSimpleName(ECPrivateKey.class));
        stringBuilder.append(" for initialisation");
        throw new InvalidKeyException(stringBuilder.toString());
    }

    private static String getSimpleName(Class clazz) {
        String fullName = clazz.getName();
        return fullName.substring(fullName.lastIndexOf(46) + 1);
    }

    protected byte[] calcSecret() {
        return bigIntToBytes(this.result);
    }
}
