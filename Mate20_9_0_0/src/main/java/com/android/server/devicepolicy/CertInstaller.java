package com.android.server.devicepolicy;

import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.List;

public class CertInstaller {
    private static final int CERTIFICATE_DEFAULT = 0;
    private static final int CERTIFICATE_WIFI = 1;
    private static final String TAG = "DPMS_CerInstall";
    private static final KeyStore mKeyStore = KeyStore.getInstance();

    public static boolean installCert(String alias, PrivateKey userKey, X509Certificate userCert, List<X509Certificate> caCerts, int certInstallType) {
        int uid;
        int uid2;
        String str = alias;
        switch (certInstallType) {
            case 0:
                if (mKeyStore.isUnlocked()) {
                    uid = -1;
                    uid2 = 1;
                    break;
                }
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Keystore is ");
                stringBuilder.append(mKeyStore.state().toString());
                stringBuilder.append(". Credentials cannot be installed until device is unlocked");
                Log.e(str2, stringBuilder.toString());
                return false;
            case 1:
                uid = 1010;
                uid2 = 0;
                break;
            default:
                List<X509Certificate> list = caCerts;
                return false;
        }
        int encryptFlag = uid2;
        uid2 = uid;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("USRPKEY_");
        stringBuilder2.append(str);
        String key = stringBuilder2.toString();
        if (userKey != null) {
            if (!mKeyStore.importKey(key, userKey.getEncoded(), uid2, encryptFlag)) {
                Log.e(TAG, "Failed to install wifi cert");
                return false;
            }
        }
        byte[] certData = null;
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("USRCERT_");
        stringBuilder2.append(str);
        String certName = stringBuilder2.toString();
        try {
            certData = Credentials.convertToPem(new Certificate[]{userCert});
        } catch (Exception e) {
            Log.e(TAG, "Failed to install convertToPem user cert");
        }
        String str3;
        if (mKeyStore.put(certName, certData, uid2, encryptFlag)) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("CACERT_");
            stringBuilder2.append(str);
            String caListName = stringBuilder2.toString();
            byte[] caListData = null;
            try {
                caListData = Credentials.convertToPem((X509Certificate[]) caCerts.toArray(new X509Certificate[caCerts.size()]));
            } catch (Exception e2) {
                Exception exception = e2;
                Log.e(TAG, "Failed to install convertToPem ca cert");
            }
            if (mKeyStore.put(caListName, caListData, uid2, encryptFlag)) {
                Log.d(TAG, "install cert success!");
                return true;
            }
            str3 = TAG;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Failed to install ");
            stringBuilder3.append(caListName);
            stringBuilder3.append(" as uid ");
            stringBuilder3.append(uid2);
            Log.e(str3, stringBuilder3.toString());
            return false;
        }
        str3 = TAG;
        StringBuilder stringBuilder4 = new StringBuilder();
        stringBuilder4.append("Failed to install ");
        stringBuilder4.append(certName);
        stringBuilder4.append(" as uid ");
        stringBuilder4.append(uid2);
        Log.e(str3, stringBuilder4.toString());
        return false;
    }
}
