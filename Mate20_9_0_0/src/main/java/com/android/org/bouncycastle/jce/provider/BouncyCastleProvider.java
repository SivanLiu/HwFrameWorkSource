package com.android.org.bouncycastle.jce.provider;

import com.android.org.bouncycastle.asn1.ASN1ObjectIdentifier;
import com.android.org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import com.android.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import com.android.org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import com.android.org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
import com.android.org.bouncycastle.jcajce.provider.util.AlgorithmProvider;
import com.android.org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public final class BouncyCastleProvider extends Provider implements ConfigurableProvider {
    private static final String[] ASYMMETRIC_CIPHERS = new String[]{"DSA", "DH", "EC", "RSA"};
    private static final String[] ASYMMETRIC_GENERIC = new String[]{"X509"};
    private static final String ASYMMETRIC_PACKAGE = "com.android.org.bouncycastle.jcajce.provider.asymmetric.";
    public static final ProviderConfiguration CONFIGURATION = new BouncyCastleProviderConfiguration();
    private static final String[] DIGESTS = new String[]{"MD5", "SHA1", "SHA224", "SHA256", "SHA384", "SHA512"};
    private static final String DIGEST_PACKAGE = "com.android.org.bouncycastle.jcajce.provider.digest.";
    private static final String[] KEYSTORES = new String[]{PROVIDER_NAME, "BCFKS", "PKCS12"};
    private static final String KEYSTORE_PACKAGE = "com.android.org.bouncycastle.jcajce.provider.keystore.";
    public static final String PROVIDER_NAME = "BC";
    private static final String[] SYMMETRIC_CIPHERS = new String[]{"AES", "ARC4", "Blowfish", "DES", "DESede", "RC2", "Twofish"};
    private static final String[] SYMMETRIC_GENERIC = new String[]{"PBEPBKDF2", "PBEPKCS12", "PBES2AlgorithmParameters"};
    private static final String[] SYMMETRIC_MACS = new String[0];
    private static final String SYMMETRIC_PACKAGE = "com.android.org.bouncycastle.jcajce.provider.symmetric.";
    private static String info = "BouncyCastle Security Provider v1.57";
    private static final Map keyInfoConverters = new HashMap();

    public BouncyCastleProvider() {
        super(PROVIDER_NAME, 1.57d, info);
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                BouncyCastleProvider.this.setup();
                return null;
            }
        });
    }

    private void setup() {
        loadAlgorithms(DIGEST_PACKAGE, DIGESTS);
        loadAlgorithms(SYMMETRIC_PACKAGE, SYMMETRIC_GENERIC);
        loadAlgorithms(SYMMETRIC_PACKAGE, SYMMETRIC_MACS);
        loadAlgorithms(SYMMETRIC_PACKAGE, SYMMETRIC_CIPHERS);
        loadAlgorithms(ASYMMETRIC_PACKAGE, ASYMMETRIC_GENERIC);
        loadAlgorithms(ASYMMETRIC_PACKAGE, ASYMMETRIC_CIPHERS);
        loadAlgorithms(KEYSTORE_PACKAGE, KEYSTORES);
        put("CertPathValidator.PKIX", "com.android.org.bouncycastle.jce.provider.PKIXCertPathValidatorSpi");
        put("CertPathBuilder.PKIX", "com.android.org.bouncycastle.jce.provider.PKIXCertPathBuilderSpi");
        put("CertStore.Collection", "com.android.org.bouncycastle.jce.provider.CertStoreCollectionSpi");
    }

    private void loadAlgorithms(String packageName, String[] names) {
        for (int i = 0; i != names.length; i++) {
            Class clazz = null;
            try {
                ClassLoader loader = getClass().getClassLoader();
                StringBuilder stringBuilder;
                if (loader != null) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(packageName);
                    stringBuilder.append(names[i]);
                    stringBuilder.append("$Mappings");
                    clazz = loader.loadClass(stringBuilder.toString());
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(packageName);
                    stringBuilder.append(names[i]);
                    stringBuilder.append("$Mappings");
                    clazz = Class.forName(stringBuilder.toString());
                }
            } catch (ClassNotFoundException e) {
            }
            if (clazz != null) {
                try {
                    ((AlgorithmProvider) clazz.newInstance()).configure(this);
                } catch (Exception e2) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("cannot create instance of ");
                    stringBuilder2.append(packageName);
                    stringBuilder2.append(names[i]);
                    stringBuilder2.append("$Mappings : ");
                    stringBuilder2.append(e2);
                    throw new InternalError(stringBuilder2.toString());
                }
            }
        }
    }

    public void setParameter(String parameterName, Object parameter) {
        synchronized (CONFIGURATION) {
            ((BouncyCastleProviderConfiguration) CONFIGURATION).setParameter(parameterName, parameter);
        }
    }

    public boolean hasAlgorithm(String type, String name) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type);
        stringBuilder.append(".");
        stringBuilder.append(name);
        if (!containsKey(stringBuilder.toString())) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.");
            stringBuilder.append(type);
            stringBuilder.append(".");
            stringBuilder.append(name);
            if (!containsKey(stringBuilder.toString())) {
                return false;
            }
        }
        return true;
    }

    public void addAlgorithm(String key, String value) {
        if (containsKey(key)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("duplicate provider key (");
            stringBuilder.append(key);
            stringBuilder.append(") found");
            throw new IllegalStateException(stringBuilder.toString());
        }
        put(key, value);
    }

    public void addAlgorithm(String type, ASN1ObjectIdentifier oid, String className) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(type);
        stringBuilder.append(".");
        stringBuilder.append(oid);
        addAlgorithm(stringBuilder.toString(), className);
        stringBuilder = new StringBuilder();
        stringBuilder.append(type);
        stringBuilder.append(".OID.");
        stringBuilder.append(oid);
        addAlgorithm(stringBuilder.toString(), className);
    }

    public void addKeyInfoConverter(ASN1ObjectIdentifier oid, AsymmetricKeyInfoConverter keyInfoConverter) {
        synchronized (keyInfoConverters) {
            keyInfoConverters.put(oid, keyInfoConverter);
        }
    }

    public void addAttributes(String key, Map<String, String> attributeMap) {
        for (String attributeName : attributeMap.keySet()) {
            String attributeKey = new StringBuilder();
            attributeKey.append(key);
            attributeKey.append(" ");
            attributeKey.append(attributeName);
            attributeKey = attributeKey.toString();
            if (containsKey(attributeKey)) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("duplicate provider attribute key (");
                stringBuilder.append(attributeKey);
                stringBuilder.append(") found");
                throw new IllegalStateException(stringBuilder.toString());
            }
            put(attributeKey, attributeMap.get(attributeName));
        }
    }

    private static AsymmetricKeyInfoConverter getAsymmetricKeyInfoConverter(ASN1ObjectIdentifier algorithm) {
        AsymmetricKeyInfoConverter asymmetricKeyInfoConverter;
        synchronized (keyInfoConverters) {
            asymmetricKeyInfoConverter = (AsymmetricKeyInfoConverter) keyInfoConverters.get(algorithm);
        }
        return asymmetricKeyInfoConverter;
    }

    public static PublicKey getPublicKey(SubjectPublicKeyInfo publicKeyInfo) throws IOException {
        AsymmetricKeyInfoConverter converter = getAsymmetricKeyInfoConverter(publicKeyInfo.getAlgorithm().getAlgorithm());
        if (converter == null) {
            return null;
        }
        return converter.generatePublic(publicKeyInfo);
    }

    public static PrivateKey getPrivateKey(PrivateKeyInfo privateKeyInfo) throws IOException {
        AsymmetricKeyInfoConverter converter = getAsymmetricKeyInfoConverter(privateKeyInfo.getPrivateKeyAlgorithm().getAlgorithm());
        if (converter == null) {
            return null;
        }
        return converter.generatePrivate(privateKeyInfo);
    }
}
