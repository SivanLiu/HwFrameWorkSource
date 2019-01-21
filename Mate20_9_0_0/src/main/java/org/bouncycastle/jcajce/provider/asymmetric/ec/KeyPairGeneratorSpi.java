package org.bouncycastle.jcajce.provider.asymmetric.ec;

import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.ECGenParameterSpec;
import java.util.Hashtable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.ECKeyPairGenerator;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECKeyGenerationParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveGenParameterSpec;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.util.Integers;

public abstract class KeyPairGeneratorSpi extends KeyPairGenerator {

    public static class EC extends KeyPairGeneratorSpi {
        private static Hashtable ecParameters = new Hashtable();
        String algorithm;
        int certainty;
        ProviderConfiguration configuration;
        Object ecParams;
        ECKeyPairGenerator engine;
        boolean initialised;
        ECKeyGenerationParameters param;
        SecureRandom random;
        int strength;

        static {
            ecParameters.put(Integers.valueOf(192), new ECGenParameterSpec("prime192v1"));
            ecParameters.put(Integers.valueOf(239), new ECGenParameterSpec("prime239v1"));
            ecParameters.put(Integers.valueOf(256), new ECGenParameterSpec("prime256v1"));
            ecParameters.put(Integers.valueOf(224), new ECGenParameterSpec("P-224"));
            ecParameters.put(Integers.valueOf(384), new ECGenParameterSpec("P-384"));
            ecParameters.put(Integers.valueOf(521), new ECGenParameterSpec("P-521"));
        }

        public EC() {
            super("EC");
            this.engine = new ECKeyPairGenerator();
            this.ecParams = null;
            this.strength = 239;
            this.certainty = 50;
            this.random = new SecureRandom();
            this.initialised = false;
            this.algorithm = "EC";
            this.configuration = BouncyCastleProvider.CONFIGURATION;
        }

        public EC(String str, ProviderConfiguration providerConfiguration) {
            super(str);
            this.engine = new ECKeyPairGenerator();
            this.ecParams = null;
            this.strength = 239;
            this.certainty = 50;
            this.random = new SecureRandom();
            this.initialised = false;
            this.algorithm = str;
            this.configuration = providerConfiguration;
        }

        protected ECKeyGenerationParameters createKeyGenParamsBC(ECParameterSpec eCParameterSpec, SecureRandom secureRandom) {
            return new ECKeyGenerationParameters(new ECDomainParameters(eCParameterSpec.getCurve(), eCParameterSpec.getG(), eCParameterSpec.getN(), eCParameterSpec.getH()), secureRandom);
        }

        protected ECKeyGenerationParameters createKeyGenParamsJCE(java.security.spec.ECParameterSpec eCParameterSpec, SecureRandom secureRandom) {
            ECCurve convertCurve = EC5Util.convertCurve(eCParameterSpec.getCurve());
            return new ECKeyGenerationParameters(new ECDomainParameters(convertCurve, EC5Util.convertPoint(convertCurve, eCParameterSpec.getGenerator(), false), eCParameterSpec.getOrder(), BigInteger.valueOf((long) eCParameterSpec.getCofactor())), secureRandom);
        }

        protected ECNamedCurveSpec createNamedCurveSpec(String str) throws InvalidAlgorithmParameterException {
            X9ECParameters domainParametersFromName = ECUtils.getDomainParametersFromName(str);
            if (domainParametersFromName == null) {
                StringBuilder stringBuilder;
                try {
                    domainParametersFromName = ECNamedCurveTable.getByOID(new ASN1ObjectIdentifier(str));
                    if (domainParametersFromName == null) {
                        domainParametersFromName = (X9ECParameters) this.configuration.getAdditionalECParameters().get(new ASN1ObjectIdentifier(str));
                        if (domainParametersFromName == null) {
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("unknown curve OID: ");
                            stringBuilder.append(str);
                            throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                        }
                    }
                } catch (IllegalArgumentException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("unknown curve name: ");
                    stringBuilder.append(str);
                    throw new InvalidAlgorithmParameterException(stringBuilder.toString());
                }
            }
            return new ECNamedCurveSpec(str, domainParametersFromName.getCurve(), domainParametersFromName.getG(), domainParametersFromName.getN(), domainParametersFromName.getH(), null);
        }

        public KeyPair generateKeyPair() {
            if (!this.initialised) {
                initialize(this.strength, new SecureRandom());
            }
            AsymmetricCipherKeyPair generateKeyPair = this.engine.generateKeyPair();
            ECPublicKeyParameters eCPublicKeyParameters = (ECPublicKeyParameters) generateKeyPair.getPublic();
            ECPrivateKeyParameters eCPrivateKeyParameters = (ECPrivateKeyParameters) generateKeyPair.getPrivate();
            BCECPublicKey bCECPublicKey;
            if (this.ecParams instanceof ECParameterSpec) {
                ECParameterSpec eCParameterSpec = (ECParameterSpec) this.ecParams;
                bCECPublicKey = new BCECPublicKey(this.algorithm, eCPublicKeyParameters, eCParameterSpec, this.configuration);
                return new KeyPair(bCECPublicKey, new BCECPrivateKey(this.algorithm, eCPrivateKeyParameters, bCECPublicKey, eCParameterSpec, this.configuration));
            } else if (this.ecParams == null) {
                return new KeyPair(new BCECPublicKey(this.algorithm, eCPublicKeyParameters, this.configuration), new BCECPrivateKey(this.algorithm, eCPrivateKeyParameters, this.configuration));
            } else {
                java.security.spec.ECParameterSpec eCParameterSpec2 = (java.security.spec.ECParameterSpec) this.ecParams;
                bCECPublicKey = new BCECPublicKey(this.algorithm, eCPublicKeyParameters, eCParameterSpec2, this.configuration);
                return new KeyPair(bCECPublicKey, new BCECPrivateKey(this.algorithm, eCPrivateKeyParameters, bCECPublicKey, eCParameterSpec2, this.configuration));
            }
        }

        public void initialize(int i, SecureRandom secureRandom) {
            this.strength = i;
            this.random = secureRandom;
            AlgorithmParameterSpec algorithmParameterSpec = (ECGenParameterSpec) ecParameters.get(Integers.valueOf(i));
            if (algorithmParameterSpec != null) {
                try {
                    initialize(algorithmParameterSpec, secureRandom);
                    return;
                } catch (InvalidAlgorithmParameterException e) {
                    throw new InvalidParameterException("key size not configurable.");
                }
            }
            throw new InvalidParameterException("unknown key size.");
        }

        public void initialize(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            ECParameterSpec ecImplicitlyCa;
            ECKeyGenerationParameters createKeyGenParamsJCE;
            if (algorithmParameterSpec == null) {
                ecImplicitlyCa = this.configuration.getEcImplicitlyCa();
                if (ecImplicitlyCa != null) {
                    this.ecParams = null;
                } else {
                    throw new InvalidAlgorithmParameterException("null parameter passed but no implicitCA set");
                }
            } else if (algorithmParameterSpec instanceof ECParameterSpec) {
                this.ecParams = algorithmParameterSpec;
                ecImplicitlyCa = (ECParameterSpec) algorithmParameterSpec;
            } else if (algorithmParameterSpec instanceof java.security.spec.ECParameterSpec) {
                this.ecParams = algorithmParameterSpec;
                createKeyGenParamsJCE = createKeyGenParamsJCE((java.security.spec.ECParameterSpec) algorithmParameterSpec, secureRandom);
                this.param = createKeyGenParamsJCE;
                this.engine.init(this.param);
                this.initialised = true;
            } else {
                String name;
                if (algorithmParameterSpec instanceof ECGenParameterSpec) {
                    name = ((ECGenParameterSpec) algorithmParameterSpec).getName();
                } else if (algorithmParameterSpec instanceof ECNamedCurveGenParameterSpec) {
                    name = ((ECNamedCurveGenParameterSpec) algorithmParameterSpec).getName();
                } else {
                    throw new InvalidAlgorithmParameterException("parameter object not a ECParameterSpec");
                }
                initializeNamedCurve(name, secureRandom);
                this.engine.init(this.param);
                this.initialised = true;
            }
            createKeyGenParamsJCE = createKeyGenParamsBC(ecImplicitlyCa, secureRandom);
            this.param = createKeyGenParamsJCE;
            this.engine.init(this.param);
            this.initialised = true;
        }

        protected void initializeNamedCurve(String str, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            ECNamedCurveSpec createNamedCurveSpec = createNamedCurveSpec(str);
            this.ecParams = createNamedCurveSpec;
            this.param = createKeyGenParamsJCE(createNamedCurveSpec, secureRandom);
        }
    }

    public static class ECDH extends EC {
        public ECDH() {
            super("ECDH", BouncyCastleProvider.CONFIGURATION);
        }
    }

    public static class ECDHC extends EC {
        public ECDHC() {
            super("ECDHC", BouncyCastleProvider.CONFIGURATION);
        }
    }

    public static class ECDSA extends EC {
        public ECDSA() {
            super("ECDSA", BouncyCastleProvider.CONFIGURATION);
        }
    }

    public static class ECMQV extends EC {
        public ECMQV() {
            super("ECMQV", BouncyCastleProvider.CONFIGURATION);
        }
    }

    public KeyPairGeneratorSpi(String str) {
        super(str);
    }
}
