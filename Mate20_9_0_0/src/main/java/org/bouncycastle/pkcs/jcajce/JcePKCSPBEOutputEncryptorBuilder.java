package org.bouncycastle.pkcs.jcajce;

import java.io.OutputStream;
import java.security.Key;
import java.security.Provider;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.PBEKeySpec;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.bc.BCObjectIdentifiers;
import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.asn1.misc.ScryptParams;
import org.bouncycastle.asn1.pkcs.EncryptionScheme;
import org.bouncycastle.asn1.pkcs.KeyDerivationFunc;
import org.bouncycastle.asn1.pkcs.PBES2Parameters;
import org.bouncycastle.asn1.pkcs.PBKDF2Params;
import org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.util.PBKDF2Config;
import org.bouncycastle.crypto.util.PBKDF2Config.Builder;
import org.bouncycastle.crypto.util.PBKDFConfig;
import org.bouncycastle.crypto.util.ScryptConfig;
import org.bouncycastle.jcajce.PKCS12KeyWithParameters;
import org.bouncycastle.jcajce.spec.ScryptKeySpec;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jcajce.util.JcaJceHelper;
import org.bouncycastle.jcajce.util.NamedJcaJceHelper;
import org.bouncycastle.jcajce.util.ProviderJcaJceHelper;
import org.bouncycastle.operator.DefaultSecretKeySizeProvider;
import org.bouncycastle.operator.GenericKey;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.SecretKeySizeProvider;

public class JcePKCSPBEOutputEncryptorBuilder {
    private ASN1ObjectIdentifier algorithm;
    private JcaJceHelper helper;
    private int iterationCount;
    private ASN1ObjectIdentifier keyEncAlgorithm;
    private SecretKeySizeProvider keySizeProvider;
    private final PBKDFConfig pbkdf;
    private Builder pbkdfBuilder;
    private SecureRandom random;

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x002c in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    public JcePKCSPBEOutputEncryptorBuilder(org.bouncycastle.asn1.ASN1ObjectIdentifier r2) {
        /*
        r1 = this;
        r1.<init>();
        r0 = new org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
        r0.<init>();
        r1.helper = r0;
        r0 = org.bouncycastle.operator.DefaultSecretKeySizeProvider.INSTANCE;
        r1.keySizeProvider = r0;
        r0 = 1024; // 0x400 float:1.435E-42 double:5.06E-321;
        r1.iterationCount = r0;
        r0 = new org.bouncycastle.crypto.util.PBKDF2Config$Builder;
        r0.<init>();
        r1.pbkdfBuilder = r0;
        r0 = 0;
        r1.pbkdf = r0;
        r0 = r1.isPKCS12(r2);
        if (r0 == 0) goto L_0x0027;
    L_0x0022:
        r1.algorithm = r2;
    L_0x0024:
        r1.keyEncAlgorithm = r2;
        return;
    L_0x0027:
        r0 = org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_PBES2;
        r1.algorithm = r0;
        goto L_0x0024;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.pkcs.jcajce.JcePKCSPBEOutputEncryptorBuilder.<init>(org.bouncycastle.asn1.ASN1ObjectIdentifier):void");
    }

    public JcePKCSPBEOutputEncryptorBuilder(PBKDFConfig pBKDFConfig, ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        this.helper = new DefaultJcaJceHelper();
        this.keySizeProvider = DefaultSecretKeySizeProvider.INSTANCE;
        this.iterationCount = 1024;
        this.pbkdfBuilder = new Builder();
        this.algorithm = PKCSObjectIdentifiers.id_PBES2;
        this.pbkdf = pBKDFConfig;
        this.keyEncAlgorithm = aSN1ObjectIdentifier;
    }

    private static byte[] PKCS12PasswordToBytes(char[] cArr) {
        int i = 0;
        if (cArr == null || cArr.length <= 0) {
            return new byte[0];
        }
        byte[] bArr = new byte[((cArr.length + 1) * 2)];
        while (i != cArr.length) {
            int i2 = i * 2;
            bArr[i2] = (byte) (cArr[i] >>> 8);
            bArr[i2 + 1] = (byte) cArr[i];
            i++;
        }
        return bArr;
    }

    private static byte[] PKCS5PasswordToBytes(char[] cArr) {
        int i = 0;
        if (cArr == null) {
            return new byte[0];
        }
        byte[] bArr = new byte[cArr.length];
        while (i != bArr.length) {
            bArr[i] = (byte) cArr[i];
            i++;
        }
        return bArr;
    }

    private boolean isPKCS12(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        return aSN1ObjectIdentifier.on(PKCSObjectIdentifiers.pkcs_12PbeIds) || aSN1ObjectIdentifier.on(BCObjectIdentifiers.bc_pbe_sha1_pkcs12) || aSN1ObjectIdentifier.on(BCObjectIdentifiers.bc_pbe_sha256_pkcs12);
    }

    public OutputEncryptor build(final char[] cArr) throws OperatorCreationException {
        if (this.random == null) {
            this.random = new SecureRandom();
        }
        try {
            Cipher createCipher;
            AlgorithmIdentifier algorithmIdentifier;
            if (isPKCS12(this.algorithm)) {
                byte[] bArr = new byte[20];
                this.random.nextBytes(bArr);
                createCipher = this.helper.createCipher(this.algorithm.getId());
                createCipher.init(1, new PKCS12KeyWithParameters(cArr, bArr, this.iterationCount));
                algorithmIdentifier = new AlgorithmIdentifier(this.algorithm, new PKCS12PBEParams(bArr, this.iterationCount));
            } else if (this.algorithm.equals(PKCSObjectIdentifiers.id_PBES2)) {
                PBKDFConfig build = this.pbkdf == null ? this.pbkdfBuilder.build() : this.pbkdf;
                if (MiscObjectIdentifiers.id_scrypt.equals(build.getAlgorithm())) {
                    ScryptConfig scryptConfig = (ScryptConfig) build;
                    byte[] bArr2 = new byte[scryptConfig.getSaltLength()];
                    this.random.nextBytes(bArr2);
                    ASN1Encodable scryptParams = new ScryptParams(bArr2, scryptConfig.getCostParameter(), scryptConfig.getBlockSize(), scryptConfig.getParallelizationParameter());
                    Key generateSecret = this.helper.createSecretKeyFactory("SCRYPT").generateSecret(new ScryptKeySpec(cArr, bArr2, scryptConfig.getCostParameter(), scryptConfig.getBlockSize(), scryptConfig.getParallelizationParameter(), this.keySizeProvider.getKeySize(new AlgorithmIdentifier(this.keyEncAlgorithm))));
                    Cipher createCipher2 = this.helper.createCipher(this.keyEncAlgorithm.getId());
                    createCipher2.init(1, generateSecret, this.random);
                    algorithmIdentifier = new AlgorithmIdentifier(this.algorithm, new PBES2Parameters(new KeyDerivationFunc(MiscObjectIdentifiers.id_scrypt, scryptParams), new EncryptionScheme(this.keyEncAlgorithm, ASN1Primitive.fromByteArray(createCipher2.getParameters().getEncoded()))));
                    createCipher = createCipher2;
                } else {
                    PBKDF2Config pBKDF2Config = (PBKDF2Config) build;
                    byte[] bArr3 = new byte[pBKDF2Config.getSaltLength()];
                    this.random.nextBytes(bArr3);
                    Key generateSecret2 = this.helper.createSecretKeyFactory(JceUtils.getAlgorithm(pBKDF2Config.getPRF().getAlgorithm())).generateSecret(new PBEKeySpec(cArr, bArr3, pBKDF2Config.getIterationCount(), this.keySizeProvider.getKeySize(new AlgorithmIdentifier(this.keyEncAlgorithm))));
                    Cipher createCipher3 = this.helper.createCipher(this.keyEncAlgorithm.getId());
                    createCipher3.init(1, generateSecret2, this.random);
                    algorithmIdentifier = new AlgorithmIdentifier(this.algorithm, new PBES2Parameters(new KeyDerivationFunc(PKCSObjectIdentifiers.id_PBKDF2, new PBKDF2Params(bArr3, pBKDF2Config.getIterationCount(), pBKDF2Config.getPRF())), new EncryptionScheme(this.keyEncAlgorithm, ASN1Primitive.fromByteArray(createCipher3.getParameters().getEncoded()))));
                    createCipher = createCipher3;
                }
            } else {
                throw new OperatorCreationException("unrecognised algorithm");
            }
            return new OutputEncryptor() {
                public AlgorithmIdentifier getAlgorithmIdentifier() {
                    return algorithmIdentifier;
                }

                public GenericKey getKey() {
                    return JcePKCSPBEOutputEncryptorBuilder.this.isPKCS12(algorithmIdentifier.getAlgorithm()) ? new GenericKey(algorithmIdentifier, JcePKCSPBEOutputEncryptorBuilder.PKCS12PasswordToBytes(cArr)) : new GenericKey(algorithmIdentifier, JcePKCSPBEOutputEncryptorBuilder.PKCS5PasswordToBytes(cArr));
                }

                public OutputStream getOutputStream(OutputStream outputStream) {
                    return new CipherOutputStream(outputStream, createCipher);
                }
            };
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unable to create OutputEncryptor: ");
            stringBuilder.append(e.getMessage());
            throw new OperatorCreationException(stringBuilder.toString(), e);
        }
    }

    public JcePKCSPBEOutputEncryptorBuilder setIterationCount(int i) {
        if (this.pbkdf == null) {
            this.iterationCount = i;
            this.pbkdfBuilder.withIterationCount(i);
            return this;
        }
        throw new IllegalStateException("set iteration count using PBKDFDef");
    }

    public JcePKCSPBEOutputEncryptorBuilder setKeySizeProvider(SecretKeySizeProvider secretKeySizeProvider) {
        this.keySizeProvider = secretKeySizeProvider;
        return this;
    }

    public JcePKCSPBEOutputEncryptorBuilder setPRF(AlgorithmIdentifier algorithmIdentifier) {
        if (this.pbkdf == null) {
            this.pbkdfBuilder.withPRF(algorithmIdentifier);
            return this;
        }
        throw new IllegalStateException("set PRF count using PBKDFDef");
    }

    public JcePKCSPBEOutputEncryptorBuilder setProvider(String str) {
        this.helper = new NamedJcaJceHelper(str);
        return this;
    }

    public JcePKCSPBEOutputEncryptorBuilder setProvider(Provider provider) {
        this.helper = new ProviderJcaJceHelper(provider);
        return this;
    }

    public JcePKCSPBEOutputEncryptorBuilder setRandom(SecureRandom secureRandom) {
        this.random = secureRandom;
        return this;
    }
}
