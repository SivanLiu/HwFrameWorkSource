package javax.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Iterator;
import sun.security.jca.GetInstance;
import sun.security.jca.GetInstance.Instance;
import sun.security.jca.Providers;

public class SecretKeyFactory {
    private final String algorithm;
    private final Object lock = new Object();
    private Provider provider;
    private Iterator<Service> serviceIterator;
    private volatile SecretKeyFactorySpi spi;

    protected SecretKeyFactory(SecretKeyFactorySpi keyFacSpi, Provider provider, String algorithm) {
        this.spi = keyFacSpi;
        this.provider = provider;
        this.algorithm = algorithm;
    }

    private SecretKeyFactory(String algorithm) throws NoSuchAlgorithmException {
        this.algorithm = algorithm;
        this.serviceIterator = GetInstance.getServices("SecretKeyFactory", algorithm).iterator();
        if (nextSpi(null) == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(algorithm);
            stringBuilder.append(" SecretKeyFactory not available");
            throw new NoSuchAlgorithmException(stringBuilder.toString());
        }
    }

    public static final SecretKeyFactory getInstance(String algorithm) throws NoSuchAlgorithmException {
        return new SecretKeyFactory(algorithm);
    }

    public static final SecretKeyFactory getInstance(String algorithm, String provider) throws NoSuchAlgorithmException, NoSuchProviderException {
        Providers.checkBouncyCastleDeprecation(provider, "SecretKeyFactory", algorithm);
        Instance instance = JceSecurity.getInstance("SecretKeyFactory", SecretKeyFactorySpi.class, algorithm, provider);
        return new SecretKeyFactory((SecretKeyFactorySpi) instance.impl, instance.provider, algorithm);
    }

    public static final SecretKeyFactory getInstance(String algorithm, Provider provider) throws NoSuchAlgorithmException {
        Providers.checkBouncyCastleDeprecation(provider, "SecretKeyFactory", algorithm);
        Instance instance = JceSecurity.getInstance("SecretKeyFactory", SecretKeyFactorySpi.class, algorithm, provider);
        return new SecretKeyFactory((SecretKeyFactorySpi) instance.impl, instance.provider, algorithm);
    }

    public final Provider getProvider() {
        Provider provider;
        synchronized (this.lock) {
            this.serviceIterator = null;
            provider = this.provider;
        }
        return provider;
    }

    public final String getAlgorithm() {
        return this.algorithm;
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x001e A:{Catch:{ NoSuchAlgorithmException -> 0x0047, all -> 0x000d }} */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x001e A:{Catch:{ NoSuchAlgorithmException -> 0x0047, all -> 0x000d }} */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0049 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x001e A:{Catch:{ NoSuchAlgorithmException -> 0x0047, all -> 0x000d }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private SecretKeyFactorySpi nextSpi(SecretKeyFactorySpi oldSpi) {
        synchronized (this.lock) {
            if (oldSpi != null) {
                try {
                    if (oldSpi != this.spi) {
                        SecretKeyFactorySpi secretKeyFactorySpi = this.spi;
                        return secretKeyFactorySpi;
                    }
                } catch (NoSuchAlgorithmException e) {
                    if (this.serviceIterator.hasNext()) {
                    }
                    this.serviceIterator = r2;
                    return r2;
                } finally {
                }
            }
            SecretKeyFactorySpi secretKeyFactorySpi2 = null;
            if (this.serviceIterator == null) {
                return null;
            }
            if (this.serviceIterator.hasNext()) {
                Service s = (Service) this.serviceIterator.next();
                if (!JceSecurity.canUseProvider(s.getProvider())) {
                    if (this.serviceIterator.hasNext()) {
                    }
                }
                SecretKeyFactorySpi obj = s.newInstance(secretKeyFactorySpi2);
                if (!(obj instanceof SecretKeyFactorySpi)) {
                    if (this.serviceIterator.hasNext()) {
                    }
                }
                SecretKeyFactorySpi spi = obj;
                this.provider = s.getProvider();
                this.spi = spi;
                return spi;
            }
            this.serviceIterator = secretKeyFactorySpi2;
            return secretKeyFactorySpi2;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0025  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0021  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final SecretKey generateSecret(KeySpec keySpec) throws InvalidKeySpecException {
        if (this.serviceIterator == null) {
            return this.spi.engineGenerateSecret(keySpec);
        }
        Exception failure = null;
        SecretKeyFactorySpi mySpi = this.spi;
        do {
            try {
                return mySpi.engineGenerateSecret(keySpec);
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                }
                mySpi = nextSpi(mySpi);
                if (mySpi == null) {
                    if (failure instanceof InvalidKeySpecException) {
                    }
                }
            }
        } while (mySpi == null);
        if (failure instanceof InvalidKeySpecException) {
            throw ((InvalidKeySpecException) failure);
        }
        throw new InvalidKeySpecException("Could not generate secret key", failure);
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0025  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0021  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final KeySpec getKeySpec(SecretKey key, Class<?> keySpec) throws InvalidKeySpecException {
        if (this.serviceIterator == null) {
            return this.spi.engineGetKeySpec(key, keySpec);
        }
        Exception failure = null;
        SecretKeyFactorySpi mySpi = this.spi;
        do {
            try {
                return mySpi.engineGetKeySpec(key, keySpec);
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                }
                mySpi = nextSpi(mySpi);
                if (mySpi == null) {
                    if (failure instanceof InvalidKeySpecException) {
                    }
                }
            }
        } while (mySpi == null);
        if (failure instanceof InvalidKeySpecException) {
            throw ((InvalidKeySpecException) failure);
        }
        throw new InvalidKeySpecException("Could not get key spec", failure);
    }

    /* JADX WARNING: Removed duplicated region for block: B:17:0x0025  */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x0021  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public final SecretKey translateKey(SecretKey key) throws InvalidKeyException {
        if (this.serviceIterator == null) {
            return this.spi.engineTranslateKey(key);
        }
        Exception failure = null;
        SecretKeyFactorySpi mySpi = this.spi;
        do {
            try {
                return mySpi.engineTranslateKey(key);
            } catch (Exception e) {
                if (failure == null) {
                    failure = e;
                }
                mySpi = nextSpi(mySpi);
                if (mySpi == null) {
                    if (failure instanceof InvalidKeyException) {
                    }
                }
            }
        } while (mySpi == null);
        if (failure instanceof InvalidKeyException) {
            throw ((InvalidKeyException) failure);
        }
        throw new InvalidKeyException("Could not translate key", failure);
    }
}
