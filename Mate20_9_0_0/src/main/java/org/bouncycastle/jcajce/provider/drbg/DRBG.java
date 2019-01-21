package org.bouncycastle.jcajce.provider.drbg;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.SecureRandomSpi;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.bouncycastle.crypto.digests.SHA512Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.prng.EntropySource;
import org.bouncycastle.crypto.prng.EntropySourceProvider;
import org.bouncycastle.crypto.prng.SP800SecureRandom;
import org.bouncycastle.crypto.prng.SP800SecureRandomBuilder;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.symmetric.util.ClassUtil;
import org.bouncycastle.jcajce.provider.util.AsymmetricAlgorithmProvider;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;
import org.bouncycastle.util.Strings;

public class DRBG {
    private static final String PREFIX = DRBG.class.getName();
    private static final Object[] initialEntropySourceAndSpi = findSource();
    private static final String[][] initialEntropySourceNames = new String[][]{new String[]{"sun.security.provider.Sun", "sun.security.provider.SecureRandom"}, new String[]{"org.apache.harmony.security.provider.crypto.CryptoProvider", "org.apache.harmony.security.provider.crypto.SHA1PRNG_SecureRandomImpl"}, new String[]{"com.android.org.conscrypt.OpenSSLProvider", "com.android.org.conscrypt.OpenSSLRandom"}, new String[]{"org.conscrypt.OpenSSLProvider", "org.conscrypt.OpenSSLRandom"}};

    private static class CoreSecureRandom extends SecureRandom {
        CoreSecureRandom() {
            super((SecureRandomSpi) DRBG.initialEntropySourceAndSpi[1], (Provider) DRBG.initialEntropySourceAndSpi[0]);
        }
    }

    public static class Default extends SecureRandomSpi {
        private static final SecureRandom random = DRBG.createBaseRandom(true);

        protected byte[] engineGenerateSeed(int i) {
            return random.generateSeed(i);
        }

        protected void engineNextBytes(byte[] bArr) {
            random.nextBytes(bArr);
        }

        protected void engineSetSeed(byte[] bArr) {
            random.setSeed(bArr);
        }
    }

    private static class HybridSecureRandom extends SecureRandom {
        private final SecureRandom baseRandom = DRBG.createInitialEntropySource();
        private final SP800SecureRandom drbg = new SP800SecureRandomBuilder(new EntropySourceProvider() {
            public EntropySource get(int i) {
                return new SignallingEntropySource(i);
            }
        }).setPersonalizationString(Strings.toByteArray("Bouncy Castle Hybrid Entropy Source")).buildHMAC(new HMac(new SHA512Digest()), this.baseRandom.generateSeed(32), false);
        private final AtomicInteger samples = new AtomicInteger(0);
        private final AtomicBoolean seedAvailable = new AtomicBoolean(false);

        private class SignallingEntropySource implements EntropySource {
            private final int byteLength;
            private final AtomicReference entropy = new AtomicReference();
            private final AtomicBoolean scheduled = new AtomicBoolean(false);

            private class EntropyGatherer implements Runnable {
                private final int numBytes;

                EntropyGatherer(int i) {
                    this.numBytes = i;
                }

                public void run() {
                    SignallingEntropySource.this.entropy.set(HybridSecureRandom.this.baseRandom.generateSeed(this.numBytes));
                    HybridSecureRandom.this.seedAvailable.set(true);
                }
            }

            SignallingEntropySource(int i) {
                this.byteLength = (i + 7) / 8;
            }

            public int entropySize() {
                return this.byteLength * 8;
            }

            public byte[] getEntropy() {
                byte[] bArr = (byte[]) this.entropy.getAndSet(null);
                if (bArr == null || bArr.length != this.byteLength) {
                    bArr = HybridSecureRandom.this.baseRandom.generateSeed(this.byteLength);
                } else {
                    this.scheduled.set(false);
                }
                if (!this.scheduled.getAndSet(true)) {
                    new Thread(new EntropyGatherer(this.byteLength)).start();
                }
                return bArr;
            }

            public boolean isPredictionResistant() {
                return true;
            }
        }

        HybridSecureRandom() {
            super(null, null);
        }

        public byte[] generateSeed(int i) {
            byte[] bArr = new byte[i];
            if (this.samples.getAndIncrement() > 20 && this.seedAvailable.getAndSet(false)) {
                this.samples.set(0);
                this.drbg.reseed((byte[]) null);
            }
            this.drbg.nextBytes(bArr);
            return bArr;
        }

        public void setSeed(long j) {
            if (this.drbg != null) {
                this.drbg.setSeed(j);
            }
        }

        public void setSeed(byte[] bArr) {
            if (this.drbg != null) {
                this.drbg.setSeed(bArr);
            }
        }
    }

    public static class NonceAndIV extends SecureRandomSpi {
        private static final SecureRandom random = DRBG.createBaseRandom(false);

        protected byte[] engineGenerateSeed(int i) {
            return random.generateSeed(i);
        }

        protected void engineNextBytes(byte[] bArr) {
            random.nextBytes(bArr);
        }

        protected void engineSetSeed(byte[] bArr) {
            random.setSeed(bArr);
        }
    }

    public static class Mappings extends AsymmetricAlgorithmProvider {
        public void configure(ConfigurableProvider configurableProvider) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(DRBG.PREFIX);
            stringBuilder.append("$Default");
            configurableProvider.addAlgorithm("SecureRandom.DEFAULT", stringBuilder.toString());
            stringBuilder = new StringBuilder();
            stringBuilder.append(DRBG.PREFIX);
            stringBuilder.append("$NonceAndIV");
            configurableProvider.addAlgorithm("SecureRandom.NONCEANDIV", stringBuilder.toString());
        }
    }

    private static SecureRandom createBaseRandom(boolean z) {
        if (System.getProperty("org.bouncycastle.drbg.entropysource") != null) {
            EntropySourceProvider createEntropySource = createEntropySource();
            EntropySource entropySource = createEntropySource.get(128);
            return new SP800SecureRandomBuilder(createEntropySource).setPersonalizationString(z ? generateDefaultPersonalizationString(entropySource.getEntropy()) : generateNonceIVPersonalizationString(entropySource.getEntropy())).buildHash(new SHA512Digest(), Arrays.concatenate(entropySource.getEntropy(), entropySource.getEntropy()), z);
        }
        HybridSecureRandom hybridSecureRandom = new HybridSecureRandom();
        return new SP800SecureRandomBuilder(hybridSecureRandom, true).setPersonalizationString(z ? generateDefaultPersonalizationString(hybridSecureRandom.generateSeed(16)) : generateNonceIVPersonalizationString(hybridSecureRandom.generateSeed(16))).buildHash(new SHA512Digest(), hybridSecureRandom.generateSeed(32), z);
    }

    private static SecureRandom createCoreSecureRandom() {
        return initialEntropySourceAndSpi != null ? new CoreSecureRandom() : new SecureRandom();
    }

    private static EntropySourceProvider createEntropySource() {
        final String property = System.getProperty("org.bouncycastle.drbg.entropysource");
        return (EntropySourceProvider) AccessController.doPrivileged(new PrivilegedAction<EntropySourceProvider>() {
            public EntropySourceProvider run() {
                try {
                    return (EntropySourceProvider) ClassUtil.loadClass(DRBG.class, property).newInstance();
                } catch (Exception e) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("entropy source ");
                    stringBuilder.append(property);
                    stringBuilder.append(" not created: ");
                    stringBuilder.append(e.getMessage());
                    throw new IllegalStateException(stringBuilder.toString(), e);
                }
            }
        });
    }

    private static SecureRandom createInitialEntropySource() {
        return ((Boolean) AccessController.doPrivileged(new PrivilegedAction<Boolean>() {
            public Boolean run() {
                try {
                    return Boolean.valueOf(SecureRandom.class.getMethod("getInstanceStrong", new Class[0]) != null);
                } catch (Exception e) {
                    return Boolean.valueOf(false);
                }
            }
        })).booleanValue() ? (SecureRandom) AccessController.doPrivileged(new PrivilegedAction<SecureRandom>() {
            public SecureRandom run() {
                try {
                    return (SecureRandom) SecureRandom.class.getMethod("getInstanceStrong", new Class[0]).invoke(null, new Object[0]);
                } catch (Exception e) {
                    return DRBG.createCoreSecureRandom();
                }
            }
        }) : createCoreSecureRandom();
    }

    private static final Object[] findSource() {
        int i = 0;
        while (i < initialEntropySourceNames.length) {
            String[] strArr = initialEntropySourceNames[i];
            try {
                return new Object[]{Class.forName(strArr[0]).newInstance(), Class.forName(strArr[1]).newInstance()};
            } catch (Throwable th) {
                i++;
            }
        }
        return null;
    }

    private static byte[] generateDefaultPersonalizationString(byte[] bArr) {
        return Arrays.concatenate(Strings.toByteArray("Default"), bArr, Pack.longToBigEndian(Thread.currentThread().getId()), Pack.longToBigEndian(System.currentTimeMillis()));
    }

    private static byte[] generateNonceIVPersonalizationString(byte[] bArr) {
        return Arrays.concatenate(Strings.toByteArray("Nonce"), bArr, Pack.longToLittleEndian(Thread.currentThread().getId()), Pack.longToLittleEndian(System.currentTimeMillis()));
    }
}
