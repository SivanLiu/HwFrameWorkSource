package com.android.org.bouncycastle.jcajce.provider.symmetric;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import com.android.org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.KeyGenerationParameters;
import com.android.org.bouncycastle.crypto.engines.DESEngine;
import com.android.org.bouncycastle.crypto.generators.DESKeyGenerator;
import com.android.org.bouncycastle.crypto.macs.CBCBlockCipherMac;
import com.android.org.bouncycastle.crypto.modes.CBCBlockCipher;
import com.android.org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import com.android.org.bouncycastle.crypto.params.DESParameters;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.BCPBEKey;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.BaseSecretKeyFactory;
import com.android.org.bouncycastle.jcajce.provider.symmetric.util.PBE.Util;
import com.android.org.bouncycastle.jcajce.provider.util.AlgorithmProvider;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class DES {

    public static class KeyGenerator extends BaseKeyGenerator {
        public KeyGenerator() {
            super("DES", 64, new DESKeyGenerator());
        }

        protected void engineInit(int keySize, SecureRandom random) {
            super.engineInit(keySize, random);
        }

        protected SecretKey engineGenerateKey() {
            if (this.uninitialised) {
                this.engine.init(new KeyGenerationParameters(new SecureRandom(), this.defaultKeySize));
                this.uninitialised = false;
            }
            return new SecretKeySpec(this.engine.generateKey(), this.algName);
        }
    }

    public static class Mappings extends AlgorithmProvider {
        private static final String PACKAGE = "com.android.org.bouncycastle.jcajce.provider.symmetric";
        private static final String PREFIX = DES.class.getName();

        public void configure(ConfigurableProvider provider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB");
            provider.addAlgorithm("Cipher.DES", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGenerator");
            provider.addAlgorithm("KeyGenerator.DES", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyFactory");
            provider.addAlgorithm("SecretKeyFactory.DES", stringBuilder.toString());
            provider.addAlgorithm("AlgorithmParameters.DES", "com.android.org.bouncycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters");
            provider.addAlgorithm("Alg.Alias.AlgorithmParameters", OIWObjectIdentifiers.desCBC, "DES");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$PBEWithMD5");
            provider.addAlgorithm("Cipher.PBEWITHMD5ANDDES", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$PBEWithSHA1");
            provider.addAlgorithm("Cipher.PBEWITHSHA1ANDDES", stringBuilder.toString());
            provider.addAlgorithm("Alg.Alias.Cipher", PKCSObjectIdentifiers.pbeWithMD5AndDES_CBC, "PBEWITHMD5ANDDES");
            provider.addAlgorithm("Alg.Alias.Cipher", PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC, "PBEWITHSHA1ANDDES");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHMD5ANDDES-CBC", "PBEWITHMD5ANDDES");
            provider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1ANDDES-CBC", "PBEWITHSHA1ANDDES");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$PBEWithMD5KeyFactory");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHMD5ANDDES", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$PBEWithSHA1KeyFactory");
            provider.addAlgorithm("SecretKeyFactory.PBEWITHSHA1ANDDES", stringBuilder.toString());
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHMD5ANDDES-CBC", "PBEWITHMD5ANDDES");
            provider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWITHSHA1ANDDES-CBC", "PBEWITHSHA1ANDDES");
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.SecretKeyFactory.");
            stringBuilder2.append(PKCSObjectIdentifiers.pbeWithMD5AndDES_CBC);
            provider.addAlgorithm(stringBuilder2.toString(), "PBEWITHMD5ANDDES");
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Alg.Alias.SecretKeyFactory.");
            stringBuilder2.append(PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC);
            provider.addAlgorithm(stringBuilder2.toString(), "PBEWITHSHA1ANDDES");
        }

        private void addAlias(ConfigurableProvider provider, ASN1ObjectIdentifier oid, String name) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.KeyGenerator.");
            stringBuilder.append(oid.getId());
            provider.addAlgorithm(stringBuilder.toString(), name);
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.KeyFactory.");
            stringBuilder.append(oid.getId());
            provider.addAlgorithm(stringBuilder.toString(), name);
        }
    }

    public static class CBCMAC extends BaseMac {
        public CBCMAC() {
            super(new CBCBlockCipherMac(new DESEngine()));
        }
    }

    public static class DES64 extends BaseMac {
        public DES64() {
            super(new CBCBlockCipherMac(new DESEngine(), 64));
        }
    }

    public static class DES64with7816d4 extends BaseMac {
        public DES64with7816d4() {
            super(new CBCBlockCipherMac(new DESEngine(), 64, new ISO7816d4Padding()));
        }
    }

    public static class DESPBEKeyFactory extends BaseSecretKeyFactory {
        private int digest;
        private boolean forCipher;
        private int ivSize;
        private int keySize;
        private int scheme;

        public DESPBEKeyFactory(String algorithm, ASN1ObjectIdentifier oid, boolean forCipher, int scheme, int digest, int keySize, int ivSize) {
            super(algorithm, oid);
            this.forCipher = forCipher;
            this.scheme = scheme;
            this.digest = digest;
            this.keySize = keySize;
            this.ivSize = ivSize;
        }

        protected SecretKey engineGenerateSecret(KeySpec keySpec) throws InvalidKeySpecException {
            if (keySpec instanceof PBEKeySpec) {
                PBEKeySpec pbeSpec = (PBEKeySpec) keySpec;
                if (pbeSpec.getSalt() == null) {
                    return new BCPBEKey(this.algName, this.algOid, this.scheme, this.digest, this.keySize, this.ivSize, pbeSpec, null);
                }
                CipherParameters param;
                KeyParameter kParam;
                if (this.forCipher) {
                    param = Util.makePBEParameters(pbeSpec, this.scheme, this.digest, this.keySize, this.ivSize);
                } else {
                    param = Util.makePBEMacParameters(pbeSpec, this.scheme, this.digest, this.keySize);
                }
                CipherParameters param2 = param;
                if (param2 instanceof ParametersWithIV) {
                    kParam = (KeyParameter) ((ParametersWithIV) param2).getParameters();
                } else {
                    kParam = (KeyParameter) param2;
                }
                DESParameters.setOddParity(kParam.getKey());
                return new BCPBEKey(this.algName, this.algOid, this.scheme, this.digest, this.keySize, this.ivSize, pbeSpec, param2);
            }
            throw new InvalidKeySpecException("Invalid KeySpec");
        }
    }

    public static class KeyFactory extends BaseSecretKeyFactory {
        public KeyFactory() {
            super("DES", null);
        }

        protected KeySpec engineGetKeySpec(SecretKey key, Class keySpec) throws InvalidKeySpecException {
            if (keySpec == null) {
                throw new InvalidKeySpecException("keySpec parameter is null");
            } else if (key == null) {
                throw new InvalidKeySpecException("key parameter is null");
            } else if (SecretKeySpec.class.isAssignableFrom(keySpec)) {
                return new SecretKeySpec(key.getEncoded(), this.algName);
            } else {
                if (DESKeySpec.class.isAssignableFrom(keySpec)) {
                    try {
                        return new DESKeySpec(key.getEncoded());
                    } catch (Exception e) {
                        throw new InvalidKeySpecException(e.toString());
                    }
                }
                throw new InvalidKeySpecException("Invalid KeySpec");
            }
        }

        protected SecretKey engineGenerateSecret(KeySpec keySpec) throws InvalidKeySpecException {
            if (keySpec instanceof DESKeySpec) {
                return new SecretKeySpec(((DESKeySpec) keySpec).getKey(), "DES");
            }
            return super.engineGenerateSecret(keySpec);
        }
    }

    public static class CBC extends BaseBlockCipher {
        public CBC() {
            super(new CBCBlockCipher(new DESEngine()), 64);
        }
    }

    public static class ECB extends BaseBlockCipher {
        public ECB() {
            super(new DESEngine());
        }
    }

    public static class PBEWithMD5 extends BaseBlockCipher {
        public PBEWithMD5() {
            CBCBlockCipher cBCBlockCipher = new CBCBlockCipher(new DESEngine());
            super(cBCBlockCipher, 0, 0, 64, 8);
        }
    }

    public static class PBEWithMD5KeyFactory extends DESPBEKeyFactory {
        public PBEWithMD5KeyFactory() {
            super("PBEwithMD5andDES", PKCSObjectIdentifiers.pbeWithMD5AndDES_CBC, true, 0, 0, 64, 64);
        }
    }

    public static class PBEWithSHA1 extends BaseBlockCipher {
        public PBEWithSHA1() {
            CBCBlockCipher cBCBlockCipher = new CBCBlockCipher(new DESEngine());
            super(cBCBlockCipher, 0, 1, 64, 8);
        }
    }

    public static class PBEWithSHA1KeyFactory extends DESPBEKeyFactory {
        public PBEWithSHA1KeyFactory() {
            super("PBEwithSHA1andDES", PKCSObjectIdentifiers.pbeWithSHA1AndDES_CBC, true, 0, 1, 64, 64);
        }
    }

    private DES() {
    }
}
