package com.android.server.locksettings.recoverablekeystore;

import java.security.Key;
import java.security.KeyStore.Entry;
import java.security.KeyStore.ProtectionParameter;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

public interface KeyStoreProxy {
    boolean containsAlias(String str) throws KeyStoreException;

    void deleteEntry(String str) throws KeyStoreException;

    Key getKey(String str, char[] cArr) throws KeyStoreException, NoSuchAlgorithmException, UnrecoverableKeyException;

    void setEntry(String str, Entry entry, ProtectionParameter protectionParameter) throws KeyStoreException;
}
