package com.android.server.locksettings.recoverablekeystore;

import android.os.RemoteException;
import android.os.ServiceSpecificException;
import android.security.keystore.recovery.TrustedRootCertificates;
import android.util.Log;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.crypto.SecretKey;

public class TestOnlyInsecureCertificateHelper {
    private static final String TAG = "TestCertHelper";

    public X509Certificate getRootCertificate(String rootCertificateAlias) throws RemoteException {
        rootCertificateAlias = getDefaultCertificateAliasIfEmpty(rootCertificateAlias);
        if (isTestOnlyCertificateAlias(rootCertificateAlias)) {
            return TrustedRootCertificates.getTestOnlyInsecureCertificate();
        }
        X509Certificate rootCertificate = TrustedRootCertificates.getRootCertificate(rootCertificateAlias);
        if (rootCertificate != null) {
            return rootCertificate;
        }
        throw new ServiceSpecificException(28, "The provided root certificate alias is invalid");
    }

    public String getDefaultCertificateAliasIfEmpty(String rootCertificateAlias) {
        if (rootCertificateAlias != null && !rootCertificateAlias.isEmpty()) {
            return rootCertificateAlias;
        }
        Log.e(TAG, "rootCertificateAlias is null or empty - use secure default value");
        return "GoogleCloudKeyVaultServiceV1";
    }

    public boolean isTestOnlyCertificateAlias(String rootCertificateAlias) {
        return "TEST_ONLY_INSECURE_CERTIFICATE_ALIAS".equals(rootCertificateAlias);
    }

    public boolean isValidRootCertificateAlias(String rootCertificateAlias) {
        return TrustedRootCertificates.getRootCertificates().containsKey(rootCertificateAlias) || isTestOnlyCertificateAlias(rootCertificateAlias);
    }

    public boolean doesCredentialSupportInsecureMode(int credentialType, String credential) {
        return credentialType == 2 && credential != null && credential.startsWith("INSECURE_PSWD_");
    }

    public Map<String, SecretKey> keepOnlyWhitelistedInsecureKeys(Map<String, SecretKey> rawKeys) {
        if (rawKeys == null) {
            return null;
        }
        Map<String, SecretKey> filteredKeys = new HashMap();
        for (Entry<String, SecretKey> entry : rawKeys.entrySet()) {
            String alias = (String) entry.getKey();
            if (alias != null && alias.startsWith("INSECURE_KEY_ALIAS_KEY_MATERIAL_IS_NOT_PROTECTED_")) {
                filteredKeys.put((String) entry.getKey(), (SecretKey) entry.getValue());
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("adding key with insecure alias ");
                stringBuilder.append(alias);
                stringBuilder.append(" to the recovery snapshot");
                Log.d(str, stringBuilder.toString());
            }
        }
        return filteredKeys;
    }
}
