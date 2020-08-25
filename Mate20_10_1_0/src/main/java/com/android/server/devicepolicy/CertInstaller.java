package com.android.server.devicepolicy;

import android.security.Credentials;
import android.security.KeyStore;
import android.util.Log;
import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;

public class CertInstaller {
    private static final int CERTIFICATE_DEFAULT = 0;
    private static final int CERTIFICATE_WIFI = 1;
    private static final String TAG = "DPMS_CerInstall";
    private static final KeyStore mKeyStore = KeyStore.getInstance();

    private CertInstaller() {
        Log.i(TAG, "Hide tool class public constructor ");
    }

    /* JADX WARNING: Removed duplicated region for block: B:43:0x0110  */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x0129  */
    public static boolean installCert(String alias, PrivateKey userKey, X509Certificate userCert, List<X509Certificate> caCerts, int certInstallType) {
        int encryptFlag;
        int encryptFlag2;
        byte[] caListData;
        if (certInstallType != 0) {
            if (certInstallType != 1) {
                return false;
            }
            encryptFlag = 0;
            encryptFlag2 = 1010;
        } else if (!mKeyStore.isUnlocked()) {
            Log.e(TAG, "Keystore is " + mKeyStore.state().toString() + ". Credentials cannot be installed until device is unlocked");
            return false;
        } else {
            encryptFlag = 1;
            encryptFlag2 = -1;
        }
        String key = "USRPKEY_" + alias;
        if (userKey != null) {
            if (!mKeyStore.importKey(key, userKey.getEncoded(), encryptFlag2, encryptFlag)) {
                Log.e(TAG, "Failed to install wifi cert");
                return false;
            }
        }
        byte[] certData = null;
        String certName = "USRCERT_" + alias;
        try {
            certData = Credentials.convertToPem(new Certificate[]{userCert});
        } catch (CertificateEncodingException e) {
            Log.e(TAG, "Failed to install convertToPem user userCert CertificateEncodingException");
        } catch (IOException e2) {
            Log.e(TAG, "Failed to install convertToPem user userCert IOException");
        } catch (Exception e3) {
            Log.e(TAG, "Failed to install convertToPem user userCert Exception");
        }
        if (!mKeyStore.put(certName, certData, encryptFlag2, encryptFlag)) {
            Log.e(TAG, "Failed to install " + certName + " as uid " + encryptFlag2);
            return false;
        }
        String caListName = "CACERT_" + alias;
        try {
            caListData = Credentials.convertToPem((X509Certificate[]) caCerts.toArray(new X509Certificate[caCerts.size()]));
        } catch (CertificateEncodingException e4) {
            Log.e(TAG, "Failed to install convertToPem user caCerts CertificateEncodingException");
            caListData = null;
            if (mKeyStore.put(caListName, caListData, encryptFlag2, encryptFlag)) {
            }
        } catch (IOException e5) {
            Log.e(TAG, "Failed to install convertToPem user caCerts IOException");
            caListData = null;
            if (mKeyStore.put(caListName, caListData, encryptFlag2, encryptFlag)) {
            }
        } catch (Exception e6) {
            Log.e(TAG, "Failed to install convertToPem user caCerts Exception");
            caListData = null;
            if (mKeyStore.put(caListName, caListData, encryptFlag2, encryptFlag)) {
            }
        }
        if (mKeyStore.put(caListName, caListData, encryptFlag2, encryptFlag)) {
            Log.e(TAG, "Failed to install " + caListName + " as uid " + encryptFlag2);
            return false;
        }
        Log.d(TAG, "install cert success!");
        return true;
    }
}
