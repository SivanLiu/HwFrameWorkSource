package com.android.server.locksettings.recoverablekeystore;

import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public class KeyStoreProxyImpl implements KeyStoreProxy {
    private static final String ANDROID_KEY_STORE_PROVIDER = "AndroidKeyStore";
    private final KeyStore mKeyStore;

    public KeyStoreProxyImpl(KeyStore keyStore) {
        this.mKeyStore = keyStore;
    }

    public boolean containsAlias(String alias) throws KeyStoreException {
        return this.mKeyStore.containsAlias(alias);
    }

    public Key getKey(String alias, char[] password) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException {
        return this.mKeyStore.getKey(alias, password);
    }

    public void setEntry(String alias, Entry entry, ProtectionParameter protParam) throws KeyStoreException {
        this.mKeyStore.setEntry(alias, entry, protParam);
    }

    public void deleteEntry(String alias) throws KeyStoreException {
        this.mKeyStore.deleteEntry(alias);
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x000c A:{Splitter: B:1:0x0007, ExcHandler: java.security.cert.CertificateException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x000c A:{Splitter: B:1:0x0007, ExcHandler: java.security.cert.CertificateException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:4:0x000c, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:6:0x0014, code:
            throw new java.security.KeyStoreException("Unable to load keystore.", r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static KeyStore getAndLoadAndroidKeyStore() throws KeyStoreException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE_PROVIDER);
        try {
            keyStore.load(null);
            return keyStore;
        } catch (Exception e) {
        }
    }
}
