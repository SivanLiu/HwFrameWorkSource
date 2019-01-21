package com.android.org.conscrypt;

import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableEntryException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

class KeyManagerImpl extends X509ExtendedKeyManager {
    private final HashMap<String, PrivateKeyEntry> hash = new HashMap();

    KeyManagerImpl(KeyStore keyStore, char[] pwd) {
        try {
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                try {
                    if (keyStore.entryInstanceOf(alias, PrivateKeyEntry.class)) {
                        this.hash.put(alias, (PrivateKeyEntry) keyStore.getEntry(alias, new PasswordProtection(pwd)));
                    }
                } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableEntryException e) {
                }
            }
        } catch (KeyStoreException e2) {
        }
    }

    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        String[] al = chooseAlias(keyTypes, issuers);
        return al == null ? null : al[0];
    }

    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        String[] al = chooseAlias(new String[]{keyType}, issuers);
        return al == null ? null : al[0];
    }

    public X509Certificate[] getCertificateChain(String alias) {
        if (alias != null && this.hash.containsKey(alias)) {
            Certificate[] certs = ((PrivateKeyEntry) this.hash.get(alias)).getCertificateChain();
            int i = 0;
            if (certs[0] instanceof X509Certificate) {
                X509Certificate[] xcerts = new X509Certificate[certs.length];
                while (i < certs.length) {
                    xcerts[i] = (X509Certificate) certs[i];
                    i++;
                }
                return xcerts;
            }
        }
        return null;
    }

    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return chooseAlias(new String[]{keyType}, issuers);
    }

    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return chooseAlias(new String[]{keyType}, issuers);
    }

    public PrivateKey getPrivateKey(String alias) {
        if (alias != null && this.hash.containsKey(alias)) {
            return ((PrivateKeyEntry) this.hash.get(alias)).getPrivateKey();
        }
        return null;
    }

    public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
        String[] al = chooseAlias(keyTypes, issuers);
        return al == null ? null : al[0];
    }

    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        String[] al = chooseAlias(new String[]{keyType}, issuers);
        return al == null ? null : al[0];
    }

    private String[] chooseAlias(String[] keyTypes, Principal[] issuers) {
        String[] strArr = keyTypes;
        Principal[] principalArr = issuers;
        if (strArr == null || strArr.length == 0) {
            return null;
        }
        List<Principal> issuersList = principalArr == null ? null : Arrays.asList(issuers);
        ArrayList<String> found = new ArrayList();
        for (Entry<String, PrivateKeyEntry> entry : this.hash.entrySet()) {
            String alias = (String) entry.getKey();
            Certificate[] chain = ((PrivateKeyEntry) entry.getValue()).getCertificateChain();
            Certificate cert = chain[0];
            String certKeyAlg = cert.getPublicKey().getAlgorithm();
            String certSigAlg;
            if (cert instanceof X509Certificate) {
                certSigAlg = ((X509Certificate) cert).getSigAlgName().toUpperCase(Locale.US);
            } else {
                certSigAlg = null;
            }
            int length = strArr.length;
            int i = 0;
            while (i < length) {
                String keyAlgorithm = strArr[i];
                if (keyAlgorithm != null) {
                    String sigAlgorithm;
                    int index = keyAlgorithm.indexOf(95);
                    if (index == -1) {
                        sigAlgorithm = null;
                    } else {
                        String sigAlgorithm2 = keyAlgorithm.substring(index + 1);
                        keyAlgorithm = keyAlgorithm.substring(null, index);
                        sigAlgorithm = sigAlgorithm2;
                    }
                    if (certKeyAlg.equals(keyAlgorithm) && (sigAlgorithm == null || certSigAlg == null || certSigAlg.contains(sigAlgorithm))) {
                        String str;
                        if (principalArr != null) {
                            if (principalArr.length == null) {
                                str = keyAlgorithm;
                            } else {
                                sigAlgorithm = chain.length;
                                int i2 = 0;
                                while (i2 < sigAlgorithm) {
                                    String str2 = sigAlgorithm;
                                    sigAlgorithm = chain[i2];
                                    str = keyAlgorithm;
                                    if ((sigAlgorithm instanceof X509Certificate) != null) {
                                        String certFromChain = sigAlgorithm;
                                        if (issuersList.contains(((X509Certificate) sigAlgorithm).getIssuerX500Principal())) {
                                            found.add(alias);
                                        }
                                    }
                                    i2++;
                                    sigAlgorithm = str2;
                                    keyAlgorithm = str;
                                }
                            }
                        } else {
                            str = keyAlgorithm;
                        }
                        found.add(alias);
                    }
                }
                i++;
                strArr = keyTypes;
                principalArr = issuers;
            }
            strArr = keyTypes;
            principalArr = issuers;
        }
        if (found.isEmpty()) {
            return null;
        }
        return (String[]) found.toArray(new String[found.size()]);
    }
}
