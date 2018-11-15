package com.android.server.wifi;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiEnterpriseConfig;
import android.security.Credentials;
import android.security.KeyChain;
import android.security.KeyStore;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import java.io.IOException;
import java.security.Key;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class WifiKeyStore {
    private static final String TAG = "WifiKeyStore";
    private final KeyStore mKeyStore;
    private boolean mVerboseLoggingEnabled = false;

    WifiKeyStore(KeyStore keyStore) {
        this.mKeyStore = keyStore;
    }

    void enableVerboseLogging(boolean verbose) {
        this.mVerboseLoggingEnabled = verbose;
    }

    private static boolean needsKeyStore(WifiEnterpriseConfig config) {
        return (config.getClientCertificate() == null && config.getCaCertificate() == null) ? false : true;
    }

    private static boolean isHardwareBackedKey(Key key) {
        return KeyChain.isBoundKeyAlgorithm(key.getAlgorithm());
    }

    private static boolean hasHardwareBackedKey(Certificate certificate) {
        return isHardwareBackedKey(certificate.getPublicKey());
    }

    private boolean installKeys(WifiEnterpriseConfig existingConfig, WifiEnterpriseConfig config, String name) {
        boolean ret;
        StringBuilder stringBuilder;
        String str = name;
        boolean ret2 = true;
        String privKeyName = new StringBuilder();
        privKeyName.append("USRPKEY_");
        privKeyName.append(str);
        privKeyName = privKeyName.toString();
        String userCertName = new StringBuilder();
        userCertName.append("USRCERT_");
        userCertName.append(str);
        userCertName = userCertName.toString();
        Certificate[] clientCertificateChain = config.getClientCertificateChain();
        int i = 1010;
        if (!(clientCertificateChain == null || clientCertificateChain.length == 0)) {
            byte[] privKeyData = config.getClientPrivateKey().getEncoded();
            if (this.mVerboseLoggingEnabled) {
                String str2;
                StringBuilder stringBuilder2;
                if (isHardwareBackedKey(config.getClientPrivateKey())) {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("importing keys ");
                    stringBuilder2.append(str);
                    stringBuilder2.append(" in hardware backed store");
                    Log.d(str2, stringBuilder2.toString());
                } else {
                    str2 = TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("importing keys ");
                    stringBuilder2.append(str);
                    stringBuilder2.append(" in software backed store");
                    Log.d(str2, stringBuilder2.toString());
                }
            }
            ret2 = this.mKeyStore.importKey(privKeyName, privKeyData, 1010, 0);
            if (!ret2) {
                return ret2;
            }
            ret2 = putCertsInKeyStore(userCertName, clientCertificateChain);
            if (!ret2) {
                this.mKeyStore.delete(privKeyName, 1010);
                return ret2;
            }
        }
        X509Certificate[] caCertificates = config.getCaCertificates();
        Set<String> oldCaCertificatesToRemove = new ArraySet();
        if (!(existingConfig == null || existingConfig.getCaCertificateAliases() == null)) {
            oldCaCertificatesToRemove.addAll(Arrays.asList(existingConfig.getCaCertificateAliases()));
        }
        List<String> caCertificateAliases = null;
        String privKeyName2;
        if (caCertificates != null) {
            caCertificateAliases = new ArrayList();
            ret = ret2;
            ret2 = false;
            while (ret2 < caCertificates.length) {
                String alias;
                if (caCertificates.length == 1) {
                    alias = str;
                } else {
                    alias = String.format("%s_%d", new Object[]{str, Integer.valueOf(ret2)});
                }
                oldCaCertificatesToRemove.remove(alias);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("CACERT_");
                stringBuilder3.append(alias);
                ret = putCertInKeyStore(stringBuilder3.toString(), caCertificates[ret2]);
                if (ret) {
                    caCertificateAliases.add(alias);
                    ret2++;
                    i = 1010;
                } else {
                    if (config.getClientCertificate() != null) {
                        this.mKeyStore.delete(privKeyName, i);
                        this.mKeyStore.delete(userCertName, i);
                    }
                    for (String addedAlias : caCertificateAliases) {
                        KeyStore keyStore = this.mKeyStore;
                        stringBuilder = new StringBuilder();
                        privKeyName2 = privKeyName;
                        stringBuilder.append("CACERT_");
                        stringBuilder.append(addedAlias);
                        keyStore.delete(stringBuilder.toString(), 1010);
                        privKeyName = privKeyName2;
                    }
                    return ret;
                }
            }
        } else {
            privKeyName2 = privKeyName;
            ret = ret2;
        }
        for (String privKeyName3 : oldCaCertificatesToRemove) {
            KeyStore keyStore2 = this.mKeyStore;
            stringBuilder = new StringBuilder();
            stringBuilder.append("CACERT_");
            stringBuilder.append(privKeyName3);
            keyStore2.delete(stringBuilder.toString(), 1010);
        }
        if (config.getClientCertificate() != null) {
            config.setClientCertificateAlias(name);
            config.resetClientKeyEntry();
        }
        if (caCertificates != null) {
            config.setCaCertificateAliases((String[]) caCertificateAliases.toArray(new String[caCertificateAliases.size()]));
            config.resetCaCertificate();
        } else {
            WifiEnterpriseConfig wifiEnterpriseConfig = config;
        }
        return ret;
    }

    public boolean putCertInKeyStore(String name, Certificate cert) {
        return putCertsInKeyStore(name, new Certificate[]{cert});
    }

    public boolean putCertsInKeyStore(String name, Certificate[] certs) {
        try {
            byte[] certData = Credentials.convertToPem(certs);
            if (this.mVerboseLoggingEnabled) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("putting ");
                stringBuilder.append(certs.length);
                stringBuilder.append(" certificate(s) ");
                stringBuilder.append(name);
                stringBuilder.append(" in keystore");
                Log.d(str, stringBuilder.toString());
            }
            return this.mKeyStore.put(name, certData, 1010, 0);
        } catch (IOException e) {
            return false;
        } catch (CertificateException e2) {
            return false;
        }
    }

    public boolean putKeyInKeyStore(String name, Key key) {
        return this.mKeyStore.importKey(name, key.getEncoded(), 1010, 0);
    }

    public boolean removeEntryFromKeyStore(String name) {
        return this.mKeyStore.delete(name, 1010);
    }

    public void removeKeys(WifiEnterpriseConfig config) {
        String client = config.getClientCertificateAlias();
        if (!TextUtils.isEmpty(client)) {
            if (this.mVerboseLoggingEnabled) {
                Log.d(TAG, "removing client private key and user cert");
            }
            KeyStore keyStore = this.mKeyStore;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("USRPKEY_");
            stringBuilder.append(client);
            keyStore.delete(stringBuilder.toString(), 1010);
            keyStore = this.mKeyStore;
            stringBuilder = new StringBuilder();
            stringBuilder.append("USRCERT_");
            stringBuilder.append(client);
            keyStore.delete(stringBuilder.toString(), 1010);
        }
        String[] aliases = config.getCaCertificateAliases();
        if (aliases != null) {
            for (String ca : aliases) {
                if (!TextUtils.isEmpty(ca)) {
                    StringBuilder stringBuilder2;
                    if (this.mVerboseLoggingEnabled) {
                        String str = TAG;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("removing CA cert: ");
                        stringBuilder2.append(ca);
                        Log.d(str, stringBuilder2.toString());
                    }
                    KeyStore keyStore2 = this.mKeyStore;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("CACERT_");
                    stringBuilder2.append(ca);
                    keyStore2.delete(stringBuilder2.toString(), 1010);
                }
            }
        }
    }

    public boolean updateNetworkKeys(WifiConfiguration config, WifiConfiguration existingConfig) {
        WifiEnterpriseConfig enterpriseConfig = config.enterpriseConfig;
        if (needsKeyStore(enterpriseConfig)) {
            String str;
            StringBuilder stringBuilder;
            try {
                if (!installKeys(existingConfig != null ? existingConfig.enterpriseConfig : null, enterpriseConfig, config.getKeyIdForCredentials(existingConfig))) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(config.SSID);
                    stringBuilder.append(": failed to install keys");
                    Log.e(str, stringBuilder.toString());
                    return false;
                }
            } catch (IllegalStateException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append(config.SSID);
                stringBuilder.append(" invalid config for key installation: ");
                stringBuilder.append(e.getMessage());
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
        return true;
    }

    public static boolean needsSoftwareBackedKeyStore(WifiEnterpriseConfig config) {
        if (TextUtils.isEmpty(config.getClientCertificateAlias())) {
            return false;
        }
        return true;
    }
}
