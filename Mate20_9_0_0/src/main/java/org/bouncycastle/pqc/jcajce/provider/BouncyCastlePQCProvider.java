package org.bouncycastle.pqc.jcajce.provider;

import java.io.IOException;
import java.security.AccessController;
import java.security.PrivateKey;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.config.ConfigurableProvider;
import org.bouncycastle.jcajce.provider.config.ProviderConfiguration;
import org.bouncycastle.jcajce.provider.util.AlgorithmProvider;
import org.bouncycastle.jcajce.provider.util.AsymmetricKeyInfoConverter;

public class BouncyCastlePQCProvider extends Provider implements ConfigurableProvider {
    private static final String[] ALGORITHMS = new String[]{"Rainbow", "McEliece", "SPHINCS", "NH", "XMSS"};
    private static final String ALGORITHM_PACKAGE = "org.bouncycastle.pqc.jcajce.provider.";
    public static final ProviderConfiguration CONFIGURATION = null;
    public static String PROVIDER_NAME = "BCPQC";
    private static String info = "BouncyCastle Post-Quantum Security Provider v1.59";
    private static final Map keyInfoConverters = new HashMap();

    public BouncyCastlePQCProvider() {
        super(PROVIDER_NAME, 1.59d, info);
        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                BouncyCastlePQCProvider.this.setup();
                return null;
            }
        });
    }

    private static AsymmetricKeyInfoConverter getAsymmetricKeyInfoConverter(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        AsymmetricKeyInfoConverter asymmetricKeyInfoConverter;
        synchronized (keyInfoConverters) {
            asymmetricKeyInfoConverter = (AsymmetricKeyInfoConverter) keyInfoConverters.get(aSN1ObjectIdentifier);
        }
        return asymmetricKeyInfoConverter;
    }

    public static PrivateKey getPrivateKey(PrivateKeyInfo privateKeyInfo) throws IOException {
        AsymmetricKeyInfoConverter asymmetricKeyInfoConverter = getAsymmetricKeyInfoConverter(privateKeyInfo.getPrivateKeyAlgorithm().getAlgorithm());
        return asymmetricKeyInfoConverter == null ? null : asymmetricKeyInfoConverter.generatePrivate(privateKeyInfo);
    }

    public static PublicKey getPublicKey(SubjectPublicKeyInfo subjectPublicKeyInfo) throws IOException {
        AsymmetricKeyInfoConverter asymmetricKeyInfoConverter = getAsymmetricKeyInfoConverter(subjectPublicKeyInfo.getAlgorithm().getAlgorithm());
        return asymmetricKeyInfoConverter == null ? null : asymmetricKeyInfoConverter.generatePublic(subjectPublicKeyInfo);
    }

    private void loadAlgorithms(String str, String[] strArr) {
        for (int i = 0; i != strArr.length; i++) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(strArr[i]);
            stringBuilder.append("$Mappings");
            Class loadClass = loadClass(BouncyCastlePQCProvider.class, stringBuilder.toString());
            if (loadClass != null) {
                try {
                    ((AlgorithmProvider) loadClass.newInstance()).configure(this);
                } catch (Exception e) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("cannot create instance of ");
                    stringBuilder2.append(str);
                    stringBuilder2.append(strArr[i]);
                    stringBuilder2.append("$Mappings : ");
                    stringBuilder2.append(e);
                    throw new InternalError(stringBuilder2.toString());
                }
            }
        }
    }

    static Class loadClass(Class cls, final String str) {
        try {
            ClassLoader classLoader = cls.getClassLoader();
            return classLoader != null ? classLoader.loadClass(str) : (Class) AccessController.doPrivileged(new PrivilegedAction() {
                public Object run() {
                    try {
                        return Class.forName(str);
                    } catch (Exception e) {
                        return null;
                    }
                }
            });
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private void setup() {
        loadAlgorithms(ALGORITHM_PACKAGE, ALGORITHMS);
    }

    public void addAlgorithm(String str, String str2) {
        if (containsKey(str)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("duplicate provider key (");
            stringBuilder.append(str);
            stringBuilder.append(") found");
            throw new IllegalStateException(stringBuilder.toString());
        }
        put(str, str2);
    }

    public void addAlgorithm(String str, ASN1ObjectIdentifier aSN1ObjectIdentifier, String str2) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(".");
        stringBuilder.append(str2);
        if (containsKey(stringBuilder.toString())) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(".");
            stringBuilder.append(aSN1ObjectIdentifier);
            addAlgorithm(stringBuilder.toString(), str2);
            stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(".OID.");
            stringBuilder.append(aSN1ObjectIdentifier);
            addAlgorithm(stringBuilder.toString(), str2);
            return;
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append("primary key (");
        stringBuilder.append(str);
        stringBuilder.append(".");
        stringBuilder.append(str2);
        stringBuilder.append(") not found");
        throw new IllegalStateException(stringBuilder.toString());
    }

    public void addAttributes(String str, Map<String, String> map) {
        for (String str2 : map.keySet()) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(str);
            stringBuilder.append(" ");
            stringBuilder.append(str2);
            String stringBuilder2 = stringBuilder.toString();
            if (containsKey(stringBuilder2)) {
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("duplicate provider attribute key (");
                stringBuilder3.append(stringBuilder2);
                stringBuilder3.append(") found");
                throw new IllegalStateException(stringBuilder3.toString());
            }
            put(stringBuilder2, map.get(str2));
        }
    }

    public void addKeyInfoConverter(ASN1ObjectIdentifier aSN1ObjectIdentifier, AsymmetricKeyInfoConverter asymmetricKeyInfoConverter) {
        synchronized (keyInfoConverters) {
            keyInfoConverters.put(aSN1ObjectIdentifier, asymmetricKeyInfoConverter);
        }
    }

    public boolean hasAlgorithm(String str, String str2) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(str);
        stringBuilder.append(".");
        stringBuilder.append(str2);
        if (!containsKey(stringBuilder.toString())) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("Alg.Alias.");
            stringBuilder.append(str);
            stringBuilder.append(".");
            stringBuilder.append(str2);
            if (!containsKey(stringBuilder.toString())) {
                return false;
            }
        }
        return true;
    }

    public void setParameter(String str, Object obj) {
        synchronized (CONFIGURATION) {
        }
    }
}
