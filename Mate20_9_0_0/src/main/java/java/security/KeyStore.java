package java.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import javax.crypto.SecretKey;
import javax.security.auth.DestroyFailedException;
import javax.security.auth.Destroyable;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;

public class KeyStore {
    private static final String KEYSTORE_TYPE = "keystore.type";
    private boolean initialized = false;
    private KeyStoreSpi keyStoreSpi;
    private Provider provider;
    private String type;

    public static abstract class Builder {
        static final int MAX_CALLBACK_TRIES = 3;

        private static final class FileBuilder extends Builder {
            private final AccessControlContext context;
            private final File file;
            private ProtectionParameter keyProtection;
            private KeyStore keyStore;
            private Throwable oldException;
            private ProtectionParameter protection;
            private final Provider provider;
            private final String type;

            FileBuilder(String type, Provider provider, File file, ProtectionParameter protection, AccessControlContext context) {
                this.type = type;
                this.provider = provider;
                this.file = file;
                this.protection = protection;
                this.context = context;
            }

            public synchronized KeyStore getKeyStore() throws KeyStoreException {
                if (this.keyStore != null) {
                    return this.keyStore;
                } else if (this.oldException == null) {
                    try {
                        this.keyStore = (KeyStore) AccessController.doPrivileged(new PrivilegedExceptionAction<KeyStore>() {
                            public KeyStore run() throws Exception {
                                if (!(FileBuilder.this.protection instanceof CallbackHandlerProtection)) {
                                    return run0();
                                }
                                int tries = 0;
                                while (true) {
                                    tries++;
                                    try {
                                        return run0();
                                    } catch (IOException e) {
                                        if (tries >= 3 || !(e.getCause() instanceof UnrecoverableKeyException)) {
                                            throw e;
                                        }
                                    }
                                }
                                throw e;
                            }

                            public KeyStore run0() throws Exception {
                                KeyStore ks;
                                if (FileBuilder.this.provider == null) {
                                    ks = KeyStore.getInstance(FileBuilder.this.type);
                                } else {
                                    ks = KeyStore.getInstance(FileBuilder.this.type, FileBuilder.this.provider);
                                }
                                InputStream in = null;
                                try {
                                    char[] password;
                                    in = new FileInputStream(FileBuilder.this.file);
                                    if (FileBuilder.this.protection instanceof PasswordProtection) {
                                        password = ((PasswordProtection) FileBuilder.this.protection).getPassword();
                                        FileBuilder.this.keyProtection = FileBuilder.this.protection;
                                    } else {
                                        CallbackHandler handler = ((CallbackHandlerProtection) FileBuilder.this.protection).getCallbackHandler();
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("Password for keystore ");
                                        stringBuilder.append(FileBuilder.this.file.getName());
                                        PasswordCallback callback = new PasswordCallback(stringBuilder.toString(), false);
                                        handler.handle(new Callback[]{callback});
                                        password = callback.getPassword();
                                        if (password != null) {
                                            callback.clearPassword();
                                            FileBuilder.this.keyProtection = new PasswordProtection(password);
                                        } else {
                                            throw new KeyStoreException("No password provided");
                                        }
                                    }
                                    ks.load(in, password);
                                    in.close();
                                    return ks;
                                } catch (Throwable th) {
                                    if (in != null) {
                                        in.close();
                                    }
                                }
                            }
                        }, this.context);
                        return this.keyStore;
                    } catch (PrivilegedActionException e) {
                        this.oldException = e.getCause();
                        throw new KeyStoreException("KeyStore instantiation failed", this.oldException);
                    }
                } else {
                    throw new KeyStoreException("Previous KeyStore instantiation failed", this.oldException);
                }
            }

            public synchronized ProtectionParameter getProtectionParameter(String alias) {
                if (alias == null) {
                    throw new NullPointerException();
                } else if (this.keyStore != null) {
                } else {
                    throw new IllegalStateException("getKeyStore() must be called first");
                }
                return this.keyProtection;
            }
        }

        public abstract KeyStore getKeyStore() throws KeyStoreException;

        public abstract ProtectionParameter getProtectionParameter(String str) throws KeyStoreException;

        protected Builder() {
        }

        public static Builder newInstance(final KeyStore keyStore, final ProtectionParameter protectionParameter) {
            if (keyStore == null || protectionParameter == null) {
                throw new NullPointerException();
            } else if (keyStore.initialized) {
                return new Builder() {
                    private volatile boolean getCalled;

                    public KeyStore getKeyStore() {
                        this.getCalled = true;
                        return keyStore;
                    }

                    public ProtectionParameter getProtectionParameter(String alias) {
                        if (alias == null) {
                            throw new NullPointerException();
                        } else if (this.getCalled) {
                            return protectionParameter;
                        } else {
                            throw new IllegalStateException("getKeyStore() must be called first");
                        }
                    }
                };
            } else {
                throw new IllegalArgumentException("KeyStore not initialized");
            }
        }

        public static Builder newInstance(String type, Provider provider, File file, ProtectionParameter protection) {
            if (type == null || file == null || protection == null) {
                throw new NullPointerException();
            } else if (!(protection instanceof PasswordProtection) && !(protection instanceof CallbackHandlerProtection)) {
                throw new IllegalArgumentException("Protection must be PasswordProtection or CallbackHandlerProtection");
            } else if (file.isFile()) {
                return new FileBuilder(type, provider, file, protection, AccessController.getContext());
            } else {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("File does not exist or it does not refer to a normal file: ");
                stringBuilder.append((Object) file);
                throw new IllegalArgumentException(stringBuilder.toString());
            }
        }

        public static Builder newInstance(final String type, final Provider provider, final ProtectionParameter protection) {
            if (type == null || protection == null) {
                throw new NullPointerException();
            }
            final AccessControlContext context = AccessController.getContext();
            return new Builder() {
                private final PrivilegedExceptionAction<KeyStore> action = new PrivilegedExceptionAction<KeyStore>() {
                    public KeyStore run() throws Exception {
                        KeyStore ks;
                        if (provider == null) {
                            ks = KeyStore.getInstance(type);
                        } else {
                            ks = KeyStore.getInstance(type, provider);
                        }
                        LoadStoreParameter param = new SimpleLoadStoreParameter(protection);
                        if (protection instanceof CallbackHandlerProtection) {
                            int tries = 0;
                            while (true) {
                                tries++;
                                try {
                                    ks.load(param);
                                    break;
                                } catch (IOException e) {
                                    if (e.getCause() instanceof UnrecoverableKeyException) {
                                        if (tries >= 3) {
                                            AnonymousClass2.this.oldException = e;
                                            break;
                                        }
                                    } else {
                                        break;
                                    }
                                    throw e;
                                }
                            }
                            throw e;
                        }
                        ks.load(param);
                        AnonymousClass2.this.getCalled = true;
                        return ks;
                    }
                };
                private volatile boolean getCalled;
                private IOException oldException;

                public synchronized KeyStore getKeyStore() throws KeyStoreException {
                    if (this.oldException == null) {
                        try {
                        } catch (PrivilegedActionException e) {
                            throw new KeyStoreException("KeyStore instantiation failed", e.getCause());
                        }
                    }
                    throw new KeyStoreException("Previous KeyStore instantiation failed", this.oldException);
                    return (KeyStore) AccessController.doPrivileged(this.action, context);
                }

                public ProtectionParameter getProtectionParameter(String alias) {
                    if (alias == null) {
                        throw new NullPointerException();
                    } else if (this.getCalled) {
                        return protection;
                    } else {
                        throw new IllegalStateException("getKeyStore() must be called first");
                    }
                }
            };
        }
    }

    public interface Entry {

        public interface Attribute {
            String getName();

            String getValue();
        }

        Set<Attribute> getAttributes() {
            return Collections.emptySet();
        }
    }

    public interface LoadStoreParameter {
        ProtectionParameter getProtectionParameter();
    }

    public interface ProtectionParameter {
    }

    public static class CallbackHandlerProtection implements ProtectionParameter {
        private final CallbackHandler handler;

        public CallbackHandlerProtection(CallbackHandler handler) {
            if (handler != null) {
                this.handler = handler;
                return;
            }
            throw new NullPointerException("handler must not be null");
        }

        public CallbackHandler getCallbackHandler() {
            return this.handler;
        }
    }

    public static class PasswordProtection implements ProtectionParameter, Destroyable {
        private volatile boolean destroyed = false;
        private final char[] password;
        private final String protectionAlgorithm;
        private final AlgorithmParameterSpec protectionParameters;

        public PasswordProtection(char[] password) {
            this.password = password == null ? null : (char[]) password.clone();
            this.protectionAlgorithm = null;
            this.protectionParameters = null;
        }

        public PasswordProtection(char[] password, String protectionAlgorithm, AlgorithmParameterSpec protectionParameters) {
            if (protectionAlgorithm != null) {
                this.password = password == null ? null : (char[]) password.clone();
                this.protectionAlgorithm = protectionAlgorithm;
                this.protectionParameters = protectionParameters;
                return;
            }
            throw new NullPointerException("invalid null input");
        }

        public String getProtectionAlgorithm() {
            return this.protectionAlgorithm;
        }

        public AlgorithmParameterSpec getProtectionParameters() {
            return this.protectionParameters;
        }

        public synchronized char[] getPassword() {
            if (this.destroyed) {
                throw new IllegalStateException("password has been cleared");
            }
            return this.password;
        }

        public synchronized void destroy() throws DestroyFailedException {
            this.destroyed = true;
            if (this.password != null) {
                Arrays.fill(this.password, ' ');
            }
        }

        public synchronized boolean isDestroyed() {
            return this.destroyed;
        }
    }

    public static final class PrivateKeyEntry implements Entry {
        private final Set<Attribute> attributes;
        private final Certificate[] chain;
        private final PrivateKey privKey;

        public PrivateKeyEntry(PrivateKey privateKey, Certificate[] chain) {
            this(privateKey, chain, Collections.emptySet());
        }

        public PrivateKeyEntry(PrivateKey privateKey, Certificate[] chain, Set<Attribute> attributes) {
            if (privateKey == null || chain == null || attributes == null) {
                throw new NullPointerException("invalid null input");
            } else if (chain.length != 0) {
                Object clonedChain = (Certificate[]) chain.clone();
                String certType = clonedChain[0].getType();
                int i = 1;
                while (i < clonedChain.length) {
                    if (certType.equals(clonedChain[i].getType())) {
                        i++;
                    } else {
                        throw new IllegalArgumentException("chain does not contain certificates of the same type");
                    }
                }
                if (privateKey.getAlgorithm().equals(clonedChain[0].getPublicKey().getAlgorithm())) {
                    this.privKey = privateKey;
                    if (!(clonedChain[0] instanceof X509Certificate) || (clonedChain instanceof X509Certificate[])) {
                        this.chain = clonedChain;
                    } else {
                        this.chain = new X509Certificate[clonedChain.length];
                        System.arraycopy(clonedChain, 0, this.chain, 0, clonedChain.length);
                    }
                    this.attributes = Collections.unmodifiableSet(new HashSet((Collection) attributes));
                    return;
                }
                throw new IllegalArgumentException("private key algorithm does not match algorithm of public key in end entity certificate (at index 0)");
            } else {
                throw new IllegalArgumentException("invalid zero-length input chain");
            }
        }

        public PrivateKey getPrivateKey() {
            return this.privKey;
        }

        public Certificate[] getCertificateChain() {
            return (Certificate[]) this.chain.clone();
        }

        public Certificate getCertificate() {
            return this.chain[0];
        }

        public Set<Attribute> getAttributes() {
            return this.attributes;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Private key entry and certificate chain with ");
            stringBuilder.append(this.chain.length);
            stringBuilder.append(" elements:\r\n");
            sb.append(stringBuilder.toString());
            for (Object cert : this.chain) {
                sb.append(cert);
                sb.append("\r\n");
            }
            return sb.toString();
        }
    }

    public static final class SecretKeyEntry implements Entry {
        private final Set<Attribute> attributes;
        private final SecretKey sKey;

        public SecretKeyEntry(SecretKey secretKey) {
            if (secretKey != null) {
                this.sKey = secretKey;
                this.attributes = Collections.emptySet();
                return;
            }
            throw new NullPointerException("invalid null input");
        }

        public SecretKeyEntry(SecretKey secretKey, Set<Attribute> attributes) {
            if (secretKey == null || attributes == null) {
                throw new NullPointerException("invalid null input");
            }
            this.sKey = secretKey;
            this.attributes = Collections.unmodifiableSet(new HashSet((Collection) attributes));
        }

        public SecretKey getSecretKey() {
            return this.sKey;
        }

        public Set<Attribute> getAttributes() {
            return this.attributes;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Secret key entry with algorithm ");
            stringBuilder.append(this.sKey.getAlgorithm());
            return stringBuilder.toString();
        }
    }

    static class SimpleLoadStoreParameter implements LoadStoreParameter {
        private final ProtectionParameter protection;

        SimpleLoadStoreParameter(ProtectionParameter protection) {
            this.protection = protection;
        }

        public ProtectionParameter getProtectionParameter() {
            return this.protection;
        }
    }

    public static final class TrustedCertificateEntry implements Entry {
        private final Set<Attribute> attributes;
        private final Certificate cert;

        public TrustedCertificateEntry(Certificate trustedCert) {
            if (trustedCert != null) {
                this.cert = trustedCert;
                this.attributes = Collections.emptySet();
                return;
            }
            throw new NullPointerException("invalid null input");
        }

        public TrustedCertificateEntry(Certificate trustedCert, Set<Attribute> attributes) {
            if (trustedCert == null || attributes == null) {
                throw new NullPointerException("invalid null input");
            }
            this.cert = trustedCert;
            this.attributes = Collections.unmodifiableSet(new HashSet((Collection) attributes));
        }

        public Certificate getTrustedCertificate() {
            return this.cert;
        }

        public Set<Attribute> getAttributes() {
            return this.attributes;
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Trusted certificate entry:\r\n");
            stringBuilder.append(this.cert.toString());
            return stringBuilder.toString();
        }
    }

    protected KeyStore(KeyStoreSpi keyStoreSpi, Provider provider, String type) {
        this.keyStoreSpi = keyStoreSpi;
        this.provider = provider;
        this.type = type;
    }

    public static KeyStore getInstance(String type) throws KeyStoreException {
        StringBuilder stringBuilder;
        try {
            Object[] objs = Security.getImpl(type, "KeyStore", (String) null);
            return new KeyStore((KeyStoreSpi) objs[0], (Provider) objs[1], type);
        } catch (NoSuchAlgorithmException nsae) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(type);
            stringBuilder.append(" not found");
            throw new KeyStoreException(stringBuilder.toString(), nsae);
        } catch (NoSuchProviderException nspe) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(type);
            stringBuilder.append(" not found");
            throw new KeyStoreException(stringBuilder.toString(), nspe);
        }
    }

    public static KeyStore getInstance(String type, String provider) throws KeyStoreException, NoSuchProviderException {
        if (provider == null || provider.length() == 0) {
            throw new IllegalArgumentException("missing provider");
        }
        try {
            Object[] objs = Security.getImpl(type, "KeyStore", provider);
            return new KeyStore((KeyStoreSpi) objs[0], (Provider) objs[1], type);
        } catch (NoSuchAlgorithmException nsae) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(type);
            stringBuilder.append(" not found");
            throw new KeyStoreException(stringBuilder.toString(), nsae);
        }
    }

    public static KeyStore getInstance(String type, Provider provider) throws KeyStoreException {
        if (provider != null) {
            try {
                Object[] objs = Security.getImpl(type, "KeyStore", provider);
                return new KeyStore((KeyStoreSpi) objs[0], (Provider) objs[1], type);
            } catch (NoSuchAlgorithmException nsae) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(type);
                stringBuilder.append(" not found");
                throw new KeyStoreException(stringBuilder.toString(), nsae);
            }
        }
        throw new IllegalArgumentException("missing provider");
    }

    public static final String getDefaultType() {
        String kstype = (String) AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return Security.getProperty(KeyStore.KEYSTORE_TYPE);
            }
        });
        if (kstype == null) {
            return "jks";
        }
        return kstype;
    }

    public final Provider getProvider() {
        return this.provider;
    }

    public final String getType() {
        return this.type;
    }

    public final Key getKey(String alias, char[] password) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        if (this.initialized) {
            return this.keyStoreSpi.engineGetKey(alias, password);
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final Certificate[] getCertificateChain(String alias) throws KeyStoreException {
        if (this.initialized) {
            return this.keyStoreSpi.engineGetCertificateChain(alias);
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final Certificate getCertificate(String alias) throws KeyStoreException {
        if (this.initialized) {
            return this.keyStoreSpi.engineGetCertificate(alias);
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final Date getCreationDate(String alias) throws KeyStoreException {
        if (this.initialized) {
            return this.keyStoreSpi.engineGetCreationDate(alias);
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final void setKeyEntry(String alias, Key key, char[] password, Certificate[] chain) throws KeyStoreException {
        if (!this.initialized) {
            throw new KeyStoreException("Uninitialized keystore");
        } else if ((key instanceof PrivateKey) && (chain == null || chain.length == 0)) {
            throw new IllegalArgumentException("Private key must be accompanied by certificate chain");
        } else {
            this.keyStoreSpi.engineSetKeyEntry(alias, key, password, chain);
        }
    }

    public final void setKeyEntry(String alias, byte[] key, Certificate[] chain) throws KeyStoreException {
        if (this.initialized) {
            this.keyStoreSpi.engineSetKeyEntry(alias, key, chain);
            return;
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final void setCertificateEntry(String alias, Certificate cert) throws KeyStoreException {
        if (this.initialized) {
            this.keyStoreSpi.engineSetCertificateEntry(alias, cert);
            return;
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final void deleteEntry(String alias) throws KeyStoreException {
        if (this.initialized) {
            this.keyStoreSpi.engineDeleteEntry(alias);
            return;
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final Enumeration<String> aliases() throws KeyStoreException {
        if (this.initialized) {
            return this.keyStoreSpi.engineAliases();
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final boolean containsAlias(String alias) throws KeyStoreException {
        if (this.initialized) {
            return this.keyStoreSpi.engineContainsAlias(alias);
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final int size() throws KeyStoreException {
        if (this.initialized) {
            return this.keyStoreSpi.engineSize();
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final boolean isKeyEntry(String alias) throws KeyStoreException {
        if (this.initialized) {
            return this.keyStoreSpi.engineIsKeyEntry(alias);
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final boolean isCertificateEntry(String alias) throws KeyStoreException {
        if (this.initialized) {
            return this.keyStoreSpi.engineIsCertificateEntry(alias);
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final String getCertificateAlias(Certificate cert) throws KeyStoreException {
        if (this.initialized) {
            return this.keyStoreSpi.engineGetCertificateAlias(cert);
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final void store(OutputStream stream, char[] password) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        if (this.initialized) {
            this.keyStoreSpi.engineStore(stream, password);
            return;
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final void store(LoadStoreParameter param) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        if (this.initialized) {
            this.keyStoreSpi.engineStore(param);
            return;
        }
        throw new KeyStoreException("Uninitialized keystore");
    }

    public final void load(InputStream stream, char[] password) throws IOException, NoSuchAlgorithmException, CertificateException {
        this.keyStoreSpi.engineLoad(stream, password);
        this.initialized = true;
    }

    public final void load(LoadStoreParameter param) throws IOException, NoSuchAlgorithmException, CertificateException {
        this.keyStoreSpi.engineLoad(param);
        this.initialized = true;
    }

    public final Entry getEntry(String alias, ProtectionParameter protParam) throws NoSuchAlgorithmException, UnrecoverableEntryException, KeyStoreException {
        if (alias == null) {
            throw new NullPointerException("invalid null input");
        } else if (this.initialized) {
            return this.keyStoreSpi.engineGetEntry(alias, protParam);
        } else {
            throw new KeyStoreException("Uninitialized keystore");
        }
    }

    public final void setEntry(String alias, Entry entry, ProtectionParameter protParam) throws KeyStoreException {
        if (alias == null || entry == null) {
            throw new NullPointerException("invalid null input");
        } else if (this.initialized) {
            this.keyStoreSpi.engineSetEntry(alias, entry, protParam);
        } else {
            throw new KeyStoreException("Uninitialized keystore");
        }
    }

    public final boolean entryInstanceOf(String alias, Class<? extends Entry> entryClass) throws KeyStoreException {
        if (alias == null || entryClass == null) {
            throw new NullPointerException("invalid null input");
        } else if (this.initialized) {
            return this.keyStoreSpi.engineEntryInstanceOf(alias, entryClass);
        } else {
            throw new KeyStoreException("Uninitialized keystore");
        }
    }
}
