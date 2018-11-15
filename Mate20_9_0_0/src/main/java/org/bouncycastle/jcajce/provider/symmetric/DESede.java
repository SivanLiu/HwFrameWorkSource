package org.bouncycastle.jcajce.provider.symmetric;

import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import javax.crypto.SecretKey;
import javax.crypto.spec.DESedeKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.engines.DESedeWrapEngine;
import org.bouncycastle.crypto.engines.RFC3211WrapEngine;
import org.bouncycastle.crypto.generators.DESedeKeyGenerator;
import org.bouncycastle.crypto.macs.CBCBlockCipherMac;
import org.bouncycastle.crypto.macs.CFBBlockCipherMac;
import org.bouncycastle.crypto.macs.CMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.ISO7816d4Padding;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.DES.DESPBEKeyFactory;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameterGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseBlockCipher;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseKeyGenerator;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseMac;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseSecretKeyFactory;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseWrapCipher;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;
import org.bouncycastle.pqc.jcajce.spec.McElieceCCA2KeyGenParameterSpec;

public final class DESede {

    public static class AlgParamGen extends BaseAlgorithmParameterGenerator {
        protected AlgorithmParameters engineGenerateParameters() {
            byte[] bArr = new byte[8];
            if (this.random == null) {
                this.random = new SecureRandom();
            }
            this.random.nextBytes(bArr);
            try {
                AlgorithmParameters createParametersInstance = createParametersInstance("DES");
                createParametersInstance.init(new IvParameterSpec(bArr));
                return createParametersInstance;
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }

        protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec, SecureRandom secureRandom) throws InvalidAlgorithmParameterException {
            throw new InvalidAlgorithmParameterException("No supported AlgorithmParameterSpec for DES parameter generation.");
        }
    }

    public static class KeyGenerator3 extends BaseKeyGenerator {
        public KeyGenerator3() {
            super("DESede3", 192, new DESedeKeyGenerator());
        }
    }

    public static class KeyGenerator extends BaseKeyGenerator {
        private boolean keySizeSet = false;

        public KeyGenerator() {
            super("DESede", 192, new DESedeKeyGenerator());
        }

        protected SecretKey engineGenerateKey() {
            if (this.uninitialised) {
                this.engine.init(new KeyGenerationParameters(new SecureRandom(), this.defaultKeySize));
                this.uninitialised = false;
            }
            if (this.keySizeSet) {
                return new SecretKeySpec(this.engine.generateKey(), this.algName);
            }
            Object generateKey = this.engine.generateKey();
            System.arraycopy(generateKey, 0, generateKey, 16, 8);
            return new SecretKeySpec(generateKey, this.algName);
        }

        protected void engineInit(int i, SecureRandom secureRandom) {
            super.engineInit(i, secureRandom);
            this.keySizeSet = true;
        }
    }

    public static class Mappings extends AlgorithmProvider {
        private static final String PACKAGE = "org.bouncycastle.jcajce.provider.symmetric";
        private static final String PREFIX = DESede.class.getName();

        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$ECB");
            configurableProvider.addAlgorithm("Cipher.DESEDE", stringBuilder.toString());
            ASN1ObjectIdentifier aSN1ObjectIdentifier = PKCSObjectIdentifiers.des_EDE3_CBC;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$CBC");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$Wrap");
            configurableProvider.addAlgorithm("Cipher.DESEDEWRAP", stringBuilder.toString());
            aSN1ObjectIdentifier = PKCSObjectIdentifiers.id_alg_CMS3DESwrap;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$Wrap");
            configurableProvider.addAlgorithm("Cipher", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$RFC3211");
            configurableProvider.addAlgorithm("Cipher.DESEDERFC3211WRAP", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.DESEDERFC3217WRAP", "DESEDEWRAP");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.TDEA", "DESEDE");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.TDEAWRAP", "DESEDEWRAP");
            configurableProvider.addAlgorithm("Alg.Alias.KeyGenerator.TDEA", "DESEDE");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.TDEA", "DESEDE");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameterGenerator.TDEA", "DESEDE");
            configurableProvider.addAlgorithm("Alg.Alias.SecretKeyFactory.TDEA", "DESEDE");
            if (configurableProvider.hasAlgorithm("MessageDigest", McElieceCCA2KeyGenParameterSpec.SHA1)) {
                stringBuilder = new StringBuilder();
                stringBuilder.append(PREFIX);
                stringBuilder.append("$PBEWithSHAAndDES3Key");
                configurableProvider.addAlgorithm("Cipher.PBEWITHSHAAND3-KEYTRIPLEDES-CBC", stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(PREFIX);
                stringBuilder.append("$BrokePBEWithSHAAndDES3Key");
                configurableProvider.addAlgorithm("Cipher.BROKENPBEWITHSHAAND3-KEYTRIPLEDES-CBC", stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(PREFIX);
                stringBuilder.append("$OldPBEWithSHAAndDES3Key");
                configurableProvider.addAlgorithm("Cipher.OLDPBEWITHSHAAND3-KEYTRIPLEDES-CBC", stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(PREFIX);
                stringBuilder.append("$PBEWithSHAAndDES2Key");
                configurableProvider.addAlgorithm("Cipher.PBEWITHSHAAND2-KEYTRIPLEDES-CBC", stringBuilder.toString());
                stringBuilder = new StringBuilder();
                stringBuilder.append(PREFIX);
                stringBuilder.append("$BrokePBEWithSHAAndDES2Key");
                configurableProvider.addAlgorithm("Cipher.BROKENPBEWITHSHAAND2-KEYTRIPLEDES-CBC", stringBuilder.toString());
                configurableProvider.addAlgorithm("Alg.Alias.Cipher", PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC, "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
                configurableProvider.addAlgorithm("Alg.Alias.Cipher", PKCSObjectIdentifiers.pbeWithSHAAnd2_KeyTripleDES_CBC, "PBEWITHSHAAND2-KEYTRIPLEDES-CBC");
                configurableProvider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1ANDDESEDE", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
                configurableProvider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND3-KEYTRIPLEDES-CBC", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
                configurableProvider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND2-KEYTRIPLEDES-CBC", "PBEWITHSHAAND2-KEYTRIPLEDES-CBC");
                configurableProvider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHAAND3-KEYDESEDE-CBC", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
                configurableProvider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHAAND2-KEYDESEDE-CBC", "PBEWITHSHAAND2-KEYTRIPLEDES-CBC");
                configurableProvider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND3-KEYDESEDE-CBC", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
                configurableProvider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1AND2-KEYDESEDE-CBC", "PBEWITHSHAAND2-KEYTRIPLEDES-CBC");
                configurableProvider.addAlgorithm("Alg.Alias.Cipher.PBEWITHSHA1ANDDESEDE-CBC", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGenerator");
            configurableProvider.addAlgorithm("KeyGenerator.DESEDE", stringBuilder.toString());
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("KeyGenerator.");
            stringBuilder3.append(PKCSObjectIdentifiers.des_EDE3_CBC);
            String stringBuilder4 = stringBuilder3.toString();
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGenerator3");
            configurableProvider.addAlgorithm(stringBuilder4, stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyGenerator");
            configurableProvider.addAlgorithm("KeyGenerator.DESEDEWRAP", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$KeyFactory");
            configurableProvider.addAlgorithm("SecretKeyFactory.DESEDE", stringBuilder.toString());
            aSN1ObjectIdentifier = OIWObjectIdentifiers.desEDE;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(PREFIX);
            stringBuilder2.append("$KeyFactory");
            configurableProvider.addAlgorithm("SecretKeyFactory", aSN1ObjectIdentifier, stringBuilder2.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$CMAC");
            configurableProvider.addAlgorithm("Mac.DESEDECMAC", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$CBCMAC");
            configurableProvider.addAlgorithm("Mac.DESEDEMAC", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.DESEDE", "DESEDEMAC");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$DESedeCFB8");
            configurableProvider.addAlgorithm("Mac.DESEDEMAC/CFB8", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.DESEDE/CFB8", "DESEDEMAC/CFB8");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$DESede64");
            configurableProvider.addAlgorithm("Mac.DESEDEMAC64", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.DESEDE64", "DESEDEMAC64");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$DESede64with7816d4");
            configurableProvider.addAlgorithm("Mac.DESEDEMAC64WITHISO7816-4PADDING", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.Mac.DESEDE64WITHISO7816-4PADDING", "DESEDEMAC64WITHISO7816-4PADDING");
            configurableProvider.addAlgorithm("Alg.Alias.Mac.DESEDEISO9797ALG1MACWITHISO7816-4PADDING", "DESEDEMAC64WITHISO7816-4PADDING");
            configurableProvider.addAlgorithm("Alg.Alias.Mac.DESEDEISO9797ALG1WITHISO7816-4PADDING", "DESEDEMAC64WITHISO7816-4PADDING");
            configurableProvider.addAlgorithm("AlgorithmParameters.DESEDE", "org.bouncycastle.jcajce.provider.symmetric.util.IvAlgorithmParameters");
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.AlgorithmParameters.");
            stringBuilder3.append(PKCSObjectIdentifiers.des_EDE3_CBC);
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "DESEDE");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$AlgParamGen");
            configurableProvider.addAlgorithm("AlgorithmParameterGenerator.DESEDE", stringBuilder.toString());
            stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Alg.Alias.AlgorithmParameterGenerator.");
            stringBuilder3.append(PKCSObjectIdentifiers.des_EDE3_CBC);
            configurableProvider.addAlgorithm(stringBuilder3.toString(), "DESEDE");
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$PBEWithSHAAndDES3KeyFactory");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND3-KEYTRIPLEDES-CBC", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(PREFIX);
            stringBuilder.append("$PBEWithSHAAndDES2KeyFactory");
            configurableProvider.addAlgorithm("SecretKeyFactory.PBEWITHSHAAND2-KEYTRIPLEDES-CBC", stringBuilder.toString());
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND3-KEYTRIPLEDES", "PKCS12PBE");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND2-KEYTRIPLEDES", "PKCS12PBE");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND3-KEYTRIPLEDES-CBC", "PKCS12PBE");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAAND2-KEYTRIPLEDES-CBC", "PKCS12PBE");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAANDDES3KEY-CBC", "PKCS12PBE");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.PBEWITHSHAANDDES2KEY-CBC", "PKCS12PBE");
            configurableProvider.addAlgorithm("Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.3", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
            configurableProvider.addAlgorithm("Alg.Alias.SecretKeyFactory.1.2.840.113549.1.12.1.4", "PBEWITHSHAAND2-KEYTRIPLEDES-CBC");
            configurableProvider.addAlgorithm("Alg.Alias.SecretKeyFactory.PBEWithSHAAnd3KeyTripleDES", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.3", "PKCS12PBE");
            configurableProvider.addAlgorithm("Alg.Alias.AlgorithmParameters.1.2.840.113549.1.12.1.4", "PKCS12PBE");
            configurableProvider.addAlgorithm("Alg.Alias.Cipher.PBEWithSHAAnd3KeyTripleDES", "PBEWITHSHAAND3-KEYTRIPLEDES-CBC");
        }
    }

    public static class CBCMAC extends BaseMac {
        public CBCMAC() {
            super(new CBCBlockCipherMac(new DESedeEngine()));
        }
    }

    public static class CMAC extends BaseMac {
        public CMAC() {
            super(new CMac(new DESedeEngine()));
        }
    }

    public static class DESede64 extends BaseMac {
        public DESede64() {
            super(new CBCBlockCipherMac(new DESedeEngine(), 64));
        }
    }

    public static class DESede64with7816d4 extends BaseMac {
        public DESede64with7816d4() {
            super(new CBCBlockCipherMac(new DESedeEngine(), 64, new ISO7816d4Padding()));
        }
    }

    public static class DESedeCFB8 extends BaseMac {
        public DESedeCFB8() {
            super(new CFBBlockCipherMac(new DESedeEngine()));
        }
    }

    public static class KeyFactory extends BaseSecretKeyFactory {
        public KeyFactory() {
            super("DESede", null);
        }

        protected SecretKey engineGenerateSecret(KeySpec keySpec) throws InvalidKeySpecException {
            return keySpec instanceof DESedeKeySpec ? new SecretKeySpec(((DESedeKeySpec) keySpec).getKey(), "DESede") : super.engineGenerateSecret(keySpec);
        }

        protected KeySpec engineGetKeySpec(SecretKey secretKey, Class cls) throws InvalidKeySpecException {
            if (cls == null) {
                throw new InvalidKeySpecException("keySpec parameter is null");
            } else if (secretKey == null) {
                throw new InvalidKeySpecException("key parameter is null");
            } else if (SecretKeySpec.class.isAssignableFrom(cls)) {
                return new SecretKeySpec(secretKey.getEncoded(), this.algName);
            } else {
                if (DESedeKeySpec.class.isAssignableFrom(cls)) {
                    Object encoded = secretKey.getEncoded();
                    try {
                        if (encoded.length != 16) {
                            return new DESedeKeySpec(encoded);
                        }
                        Object obj = new byte[24];
                        System.arraycopy(encoded, 0, obj, 0, 16);
                        System.arraycopy(encoded, 0, obj, 16, 8);
                        return new DESedeKeySpec(obj);
                    } catch (Exception e) {
                        throw new InvalidKeySpecException(e.toString());
                    }
                }
                throw new InvalidKeySpecException("Invalid KeySpec");
            }
        }
    }

    public static class RFC3211 extends BaseWrapCipher {
        public RFC3211() {
            super(new RFC3211WrapEngine(new DESedeEngine()), 8);
        }
    }

    public static class Wrap extends BaseWrapCipher {
        public Wrap() {
            super(new DESedeWrapEngine());
        }
    }

    public static class CBC extends BaseBlockCipher {
        public CBC() {
            super(new CBCBlockCipher(new DESedeEngine()), 64);
        }
    }

    public static class ECB extends BaseBlockCipher {
        public ECB() {
            super(new DESedeEngine());
        }
    }

    public static class PBEWithSHAAndDES2Key extends BaseBlockCipher {
        public PBEWithSHAAndDES2Key() {
            BlockCipher cBCBlockCipher = new CBCBlockCipher(new DESedeEngine());
            super(cBCBlockCipher, 2, 1, 128, 8);
        }
    }

    public static class PBEWithSHAAndDES2KeyFactory extends DESPBEKeyFactory {
        public PBEWithSHAAndDES2KeyFactory() {
            super("PBEwithSHAandDES2Key-CBC", PKCSObjectIdentifiers.pbeWithSHAAnd2_KeyTripleDES_CBC, true, 2, 1, 128, 64);
        }
    }

    public static class PBEWithSHAAndDES3Key extends BaseBlockCipher {
        public PBEWithSHAAndDES3Key() {
            BlockCipher cBCBlockCipher = new CBCBlockCipher(new DESedeEngine());
            super(cBCBlockCipher, 2, 1, 192, 8);
        }
    }

    public static class PBEWithSHAAndDES3KeyFactory extends DESPBEKeyFactory {
        public PBEWithSHAAndDES3KeyFactory() {
            super("PBEwithSHAandDES3Key-CBC", PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC, true, 2, 1, 192, 64);
        }
    }

    private DESede() {
    }
}
