package org.bouncycastle.jcajce.provider.asymmetric.ec;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.BasicAgreement;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DerivationFunction;
import org.bouncycastle.crypto.agreement.ECDHBasicAgreement;
import org.bouncycastle.crypto.agreement.ECDHCBasicAgreement;
import org.bouncycastle.crypto.agreement.ECDHCUnifiedAgreement;
import org.bouncycastle.crypto.agreement.ECMQVBasicAgreement;
import org.bouncycastle.crypto.agreement.kdf.ConcatenationKDFGenerator;
import org.bouncycastle.crypto.generators.KDF2BytesGenerator;
import org.bouncycastle.crypto.params.ECDHUPrivateParameters;
import org.bouncycastle.crypto.params.ECDHUPublicParameters;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.params.MQVPrivateParameters;
import org.bouncycastle.crypto.params.MQVPublicParameters;
import org.bouncycastle.crypto.util.DigestFactory;
import org.bouncycastle.jcajce.provider.asymmetric.util.BaseAgreementSpi;
import org.bouncycastle.jcajce.provider.asymmetric.util.ECUtil;
import org.bouncycastle.jcajce.spec.DHUParameterSpec;
import org.bouncycastle.jcajce.spec.MQVParameterSpec;
import org.bouncycastle.jcajce.spec.UserKeyingMaterialSpec;
import org.bouncycastle.jce.interfaces.ECPrivateKey;
import org.bouncycastle.jce.interfaces.ECPublicKey;
import org.bouncycastle.jce.interfaces.MQVPrivateKey;
import org.bouncycastle.jce.interfaces.MQVPublicKey;
import org.bouncycastle.util.Arrays;

public class KeyAgreementSpi extends BaseAgreementSpi {
    private static final X9IntegerConverter converter = new X9IntegerConverter();
    private Object agreement;
    private DHUParameterSpec dheParameters;
    private String kaAlgorithm;
    private MQVParameterSpec mqvParameters;
    private ECDomainParameters parameters;
    private byte[] result;

    public static class CDHwithSHA1KDFAndSharedInfo extends KeyAgreementSpi {
        public CDHwithSHA1KDFAndSharedInfo() {
            super("ECCDHwithSHA1KDF", new ECDHCBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA1()));
        }
    }

    public static class CDHwithSHA224KDFAndSharedInfo extends KeyAgreementSpi {
        public CDHwithSHA224KDFAndSharedInfo() {
            super("ECCDHwithSHA224KDF", new ECDHCBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA224()));
        }
    }

    public static class CDHwithSHA256KDFAndSharedInfo extends KeyAgreementSpi {
        public CDHwithSHA256KDFAndSharedInfo() {
            super("ECCDHwithSHA256KDF", new ECDHCBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA256()));
        }
    }

    public static class CDHwithSHA384KDFAndSharedInfo extends KeyAgreementSpi {
        public CDHwithSHA384KDFAndSharedInfo() {
            super("ECCDHwithSHA384KDF", new ECDHCBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA384()));
        }
    }

    public static class CDHwithSHA512KDFAndSharedInfo extends KeyAgreementSpi {
        public CDHwithSHA512KDFAndSharedInfo() {
            super("ECCDHwithSHA512KDF", new ECDHCBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA512()));
        }
    }

    public static class DH extends KeyAgreementSpi {
        public DH() {
            super("ECDH", new ECDHBasicAgreement(), null);
        }
    }

    public static class DHC extends KeyAgreementSpi {
        public DHC() {
            super("ECDHC", new ECDHCBasicAgreement(), null);
        }
    }

    public static class DHUC extends KeyAgreementSpi {
        public DHUC() {
            super("ECCDHU", new ECDHCUnifiedAgreement(), null);
        }
    }

    public static class DHUwithSHA1CKDF extends KeyAgreementSpi {
        public DHUwithSHA1CKDF() {
            super("ECCDHUwithSHA1CKDF", new ECDHCUnifiedAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA1()));
        }
    }

    public static class DHUwithSHA224CKDF extends KeyAgreementSpi {
        public DHUwithSHA224CKDF() {
            super("ECCDHUwithSHA224CKDF", new ECDHCUnifiedAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA224()));
        }
    }

    public static class DHUwithSHA256CKDF extends KeyAgreementSpi {
        public DHUwithSHA256CKDF() {
            super("ECCDHUwithSHA256CKDF", new ECDHCUnifiedAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA256()));
        }
    }

    public static class DHUwithSHA384CKDF extends KeyAgreementSpi {
        public DHUwithSHA384CKDF() {
            super("ECCDHUwithSHA384CKDF", new ECDHCUnifiedAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA384()));
        }
    }

    public static class DHUwithSHA512CKDF extends KeyAgreementSpi {
        public DHUwithSHA512CKDF() {
            super("ECCDHUwithSHA512CKDF", new ECDHCUnifiedAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA512()));
        }
    }

    public static class DHwithSHA1CKDF extends KeyAgreementSpi {
        public DHwithSHA1CKDF() {
            super("ECDHwithSHA1CKDF", new ECDHCBasicAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA1()));
        }
    }

    public static class DHwithSHA1KDF extends KeyAgreementSpi {
        public DHwithSHA1KDF() {
            super("ECDHwithSHA1KDF", new ECDHBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA1()));
        }
    }

    public static class DHwithSHA1KDFAndSharedInfo extends KeyAgreementSpi {
        public DHwithSHA1KDFAndSharedInfo() {
            super("ECDHwithSHA1KDF", new ECDHBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA1()));
        }
    }

    public static class DHwithSHA224KDFAndSharedInfo extends KeyAgreementSpi {
        public DHwithSHA224KDFAndSharedInfo() {
            super("ECDHwithSHA224KDF", new ECDHBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA224()));
        }
    }

    public static class DHwithSHA256CKDF extends KeyAgreementSpi {
        public DHwithSHA256CKDF() {
            super("ECDHwithSHA256CKDF", new ECDHCBasicAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA256()));
        }
    }

    public static class DHwithSHA256KDFAndSharedInfo extends KeyAgreementSpi {
        public DHwithSHA256KDFAndSharedInfo() {
            super("ECDHwithSHA256KDF", new ECDHBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA256()));
        }
    }

    public static class DHwithSHA384CKDF extends KeyAgreementSpi {
        public DHwithSHA384CKDF() {
            super("ECDHwithSHA384CKDF", new ECDHCBasicAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA384()));
        }
    }

    public static class DHwithSHA384KDFAndSharedInfo extends KeyAgreementSpi {
        public DHwithSHA384KDFAndSharedInfo() {
            super("ECDHwithSHA384KDF", new ECDHBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA384()));
        }
    }

    public static class DHwithSHA512CKDF extends KeyAgreementSpi {
        public DHwithSHA512CKDF() {
            super("ECDHwithSHA512CKDF", new ECDHCBasicAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA512()));
        }
    }

    public static class DHwithSHA512KDFAndSharedInfo extends KeyAgreementSpi {
        public DHwithSHA512KDFAndSharedInfo() {
            super("ECDHwithSHA512KDF", new ECDHBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA512()));
        }
    }

    public static class MQV extends KeyAgreementSpi {
        public MQV() {
            super("ECMQV", new ECMQVBasicAgreement(), null);
        }
    }

    public static class MQVwithSHA1CKDF extends KeyAgreementSpi {
        public MQVwithSHA1CKDF() {
            super("ECMQVwithSHA1CKDF", new ECMQVBasicAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA1()));
        }
    }

    public static class MQVwithSHA1KDFAndSharedInfo extends KeyAgreementSpi {
        public MQVwithSHA1KDFAndSharedInfo() {
            super("ECMQVwithSHA1KDF", new ECMQVBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA1()));
        }
    }

    public static class MQVwithSHA224CKDF extends KeyAgreementSpi {
        public MQVwithSHA224CKDF() {
            super("ECMQVwithSHA224CKDF", new ECMQVBasicAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA224()));
        }
    }

    public static class MQVwithSHA224KDFAndSharedInfo extends KeyAgreementSpi {
        public MQVwithSHA224KDFAndSharedInfo() {
            super("ECMQVwithSHA224KDF", new ECMQVBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA224()));
        }
    }

    public static class MQVwithSHA256CKDF extends KeyAgreementSpi {
        public MQVwithSHA256CKDF() {
            super("ECMQVwithSHA256CKDF", new ECMQVBasicAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA256()));
        }
    }

    public static class MQVwithSHA256KDFAndSharedInfo extends KeyAgreementSpi {
        public MQVwithSHA256KDFAndSharedInfo() {
            super("ECMQVwithSHA256KDF", new ECMQVBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA256()));
        }
    }

    public static class MQVwithSHA384CKDF extends KeyAgreementSpi {
        public MQVwithSHA384CKDF() {
            super("ECMQVwithSHA384CKDF", new ECMQVBasicAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA384()));
        }
    }

    public static class MQVwithSHA384KDFAndSharedInfo extends KeyAgreementSpi {
        public MQVwithSHA384KDFAndSharedInfo() {
            super("ECMQVwithSHA384KDF", new ECMQVBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA384()));
        }
    }

    public static class MQVwithSHA512CKDF extends KeyAgreementSpi {
        public MQVwithSHA512CKDF() {
            super("ECDHUwithSHA512CKDF", new ECMQVBasicAgreement(), new ConcatenationKDFGenerator(DigestFactory.createSHA512()));
        }
    }

    public static class MQVwithSHA512KDFAndSharedInfo extends KeyAgreementSpi {
        public MQVwithSHA512KDFAndSharedInfo() {
            super("ECMQVwithSHA512KDF", new ECMQVBasicAgreement(), new KDF2BytesGenerator(DigestFactory.createSHA512()));
        }
    }

    protected KeyAgreementSpi(String str, BasicAgreement basicAgreement, DerivationFunction derivationFunction) {
        super(str, derivationFunction);
        this.kaAlgorithm = str;
        this.agreement = basicAgreement;
    }

    protected KeyAgreementSpi(String str, ECDHCUnifiedAgreement eCDHCUnifiedAgreement, DerivationFunction derivationFunction) {
        super(str, derivationFunction);
        this.kaAlgorithm = str;
        this.agreement = eCDHCUnifiedAgreement;
    }

    private static String getSimpleName(Class cls) {
        String name = cls.getName();
        return name.substring(name.lastIndexOf(46) + 1);
    }

    private void initFromKey(Key key, AlgorithmParameterSpec algorithmParameterSpec) throws InvalidKeyException {
        byte[] bArr = null;
        ECPrivateKeyParameters eCPrivateKeyParameters;
        ECPublicKeyParameters eCPublicKeyParameters;
        ECPrivateKeyParameters eCPrivateKeyParameters2;
        CipherParameters mQVPrivateParameters;
        StringBuilder stringBuilder;
        if (this.agreement instanceof ECMQVBasicAgreement) {
            this.mqvParameters = null;
            boolean z = key instanceof MQVPrivateKey;
            if (z || (algorithmParameterSpec instanceof MQVParameterSpec)) {
                if (z) {
                    MQVPrivateKey mQVPrivateKey = (MQVPrivateKey) key;
                    ECPrivateKeyParameters eCPrivateKeyParameters3 = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter(mQVPrivateKey.getStaticPrivateKey());
                    eCPrivateKeyParameters = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter(mQVPrivateKey.getEphemeralPrivateKey());
                    if (mQVPrivateKey.getEphemeralPublicKey() != null) {
                        eCPublicKeyParameters = (ECPublicKeyParameters) ECUtils.generatePublicKeyParameter(mQVPrivateKey.getEphemeralPublicKey());
                    }
                    eCPrivateKeyParameters2 = eCPrivateKeyParameters3;
                } else {
                    MQVParameterSpec mQVParameterSpec = (MQVParameterSpec) algorithmParameterSpec;
                    eCPrivateKeyParameters2 = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter((PrivateKey) key);
                    eCPrivateKeyParameters = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter(mQVParameterSpec.getEphemeralPrivateKey());
                    if (mQVParameterSpec.getEphemeralPublicKey() != null) {
                        eCPublicKeyParameters = (ECPublicKeyParameters) ECUtils.generatePublicKeyParameter(mQVParameterSpec.getEphemeralPublicKey());
                    }
                    this.mqvParameters = mQVParameterSpec;
                    this.ukmParameters = mQVParameterSpec.getUserKeyingMaterial();
                }
                mQVPrivateParameters = new MQVPrivateParameters(eCPrivateKeyParameters2, eCPrivateKeyParameters, eCPublicKeyParameters);
                this.parameters = eCPrivateKeyParameters2.getParameters();
                ((ECMQVBasicAgreement) this.agreement).init(mQVPrivateParameters);
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.kaAlgorithm);
            stringBuilder.append(" key agreement requires ");
            stringBuilder.append(getSimpleName(MQVParameterSpec.class));
            stringBuilder.append(" for initialisation");
            throw new InvalidKeyException(stringBuilder.toString());
        } else if (algorithmParameterSpec instanceof DHUParameterSpec) {
            if (this.agreement instanceof ECDHCUnifiedAgreement) {
                DHUParameterSpec dHUParameterSpec = (DHUParameterSpec) algorithmParameterSpec;
                eCPrivateKeyParameters2 = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter((PrivateKey) key);
                eCPrivateKeyParameters = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter(dHUParameterSpec.getEphemeralPrivateKey());
                if (dHUParameterSpec.getEphemeralPublicKey() != null) {
                    eCPublicKeyParameters = (ECPublicKeyParameters) ECUtils.generatePublicKeyParameter(dHUParameterSpec.getEphemeralPublicKey());
                }
                this.dheParameters = dHUParameterSpec;
                this.ukmParameters = dHUParameterSpec.getUserKeyingMaterial();
                mQVPrivateParameters = new ECDHUPrivateParameters(eCPrivateKeyParameters2, eCPrivateKeyParameters, eCPublicKeyParameters);
                this.parameters = eCPrivateKeyParameters2.getParameters();
                ((ECDHCUnifiedAgreement) this.agreement).init(mQVPrivateParameters);
                return;
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.kaAlgorithm);
            stringBuilder.append(" key agreement cannot be used with ");
            stringBuilder.append(getSimpleName(DHUParameterSpec.class));
            throw new InvalidKeyException(stringBuilder.toString());
        } else if (key instanceof PrivateKey) {
            eCPrivateKeyParameters2 = (ECPrivateKeyParameters) ECUtil.generatePrivateKeyParameter((PrivateKey) key);
            this.parameters = eCPrivateKeyParameters2.getParameters();
            if (algorithmParameterSpec instanceof UserKeyingMaterialSpec) {
                bArr = ((UserKeyingMaterialSpec) algorithmParameterSpec).getUserKeyingMaterial();
            }
            this.ukmParameters = bArr;
            ((BasicAgreement) this.agreement).init(eCPrivateKeyParameters2);
        } else {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.kaAlgorithm);
            stringBuilder.append(" key agreement requires ");
            stringBuilder.append(getSimpleName(ECPrivateKey.class));
            stringBuilder.append(" for initialisation");
            throw new InvalidKeyException(stringBuilder.toString());
        }
    }

    protected byte[] bigIntToBytes(BigInteger bigInteger) {
        return converter.integerToBytes(bigInteger, converter.getByteLength(this.parameters.getCurve()));
    }

    protected byte[] calcSecret() {
        return Arrays.clone(this.result);
    }

    protected Key engineDoPhase(Key key, boolean z) throws InvalidKeyException, IllegalStateException {
        StringBuilder stringBuilder;
        if (this.parameters == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(this.kaAlgorithm);
            stringBuilder.append(" not initialised.");
            throw new IllegalStateException(stringBuilder.toString());
        } else if (z) {
            CipherParameters mQVPublicParameters;
            if (this.agreement instanceof ECMQVBasicAgreement) {
                if (key instanceof MQVPublicKey) {
                    MQVPublicKey mQVPublicKey = (MQVPublicKey) key;
                    mQVPublicParameters = new MQVPublicParameters((ECPublicKeyParameters) ECUtils.generatePublicKeyParameter(mQVPublicKey.getStaticKey()), (ECPublicKeyParameters) ECUtils.generatePublicKeyParameter(mQVPublicKey.getEphemeralKey()));
                } else {
                    mQVPublicParameters = new MQVPublicParameters((ECPublicKeyParameters) ECUtils.generatePublicKeyParameter((PublicKey) key), (ECPublicKeyParameters) ECUtils.generatePublicKeyParameter(this.mqvParameters.getOtherPartyEphemeralKey()));
                }
            } else if (this.agreement instanceof ECDHCUnifiedAgreement) {
                mQVPublicParameters = new ECDHUPublicParameters((ECPublicKeyParameters) ECUtils.generatePublicKeyParameter((PublicKey) key), (ECPublicKeyParameters) ECUtils.generatePublicKeyParameter(this.dheParameters.getOtherPartyEphemeralKey()));
            } else if (key instanceof PublicKey) {
                mQVPublicParameters = ECUtils.generatePublicKeyParameter((PublicKey) key);
            } else {
                stringBuilder = new StringBuilder();
                stringBuilder.append(this.kaAlgorithm);
                stringBuilder.append(" key agreement requires ");
                stringBuilder.append(getSimpleName(ECPublicKey.class));
                stringBuilder.append(" for doPhase");
                throw new InvalidKeyException(stringBuilder.toString());
            }
            try {
                this.result = this.agreement instanceof BasicAgreement ? bigIntToBytes(((BasicAgreement) this.agreement).calculateAgreement(mQVPublicParameters)) : ((ECDHCUnifiedAgreement) this.agreement).calculateAgreement(mQVPublicParameters);
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
            stringBuilder.append(" can only be between two parties.");
            throw new IllegalStateException(stringBuilder.toString());
        }
    }

    protected void engineInit(Key key, SecureRandom secureRandom) throws InvalidKeyException {
        initFromKey(key, null);
    }

    protected void engineInit(Key key, AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidKeyException, InvalidAlgorithmParameterException {
        if (algorithmParameterSpec == null || (algorithmParameterSpec instanceof MQVParameterSpec) || (algorithmParameterSpec instanceof UserKeyingMaterialSpec) || (algorithmParameterSpec instanceof DHUParameterSpec)) {
            initFromKey(key, algorithmParameterSpec);
            return;
        }
        throw new InvalidAlgorithmParameterException("No algorithm parameters supported");
    }
}
