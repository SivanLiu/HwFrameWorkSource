package com.android.server.wifi.hotspot2;

import android.os.Environment;
import android.util.Log;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class WfaKeyStore {
    private static final String DEFAULT_WFA_CERT_DIR;
    private static final String TAG = "WfaKeyStore";
    private KeyStore mKeyStore = null;
    private boolean mVerboseLoggingEnabled = false;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(Environment.getRootDirectory());
        stringBuilder.append("/etc/security/cacerts_wfa");
        DEFAULT_WFA_CERT_DIR = stringBuilder.toString();
    }

    /* JADX WARNING: Removed duplicated region for block: B:11:0x004e A:{Splitter: B:4:0x0007, ExcHandler: java.security.KeyStoreException (r0_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x004e A:{Splitter: B:4:0x0007, ExcHandler: java.security.KeyStoreException (r0_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:11:0x004e A:{Splitter: B:4:0x0007, ExcHandler: java.security.KeyStoreException (r0_3 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:11:0x004e, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:12:0x004f, code:
            r0.printStackTrace();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void load() {
        if (this.mKeyStore == null) {
            int index = 0;
            try {
                this.mKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                this.mKeyStore.load(null, null);
                for (X509Certificate cert : WfaCertBuilder.loadCertsFromDisk(DEFAULT_WFA_CERT_DIR)) {
                    this.mKeyStore.setCertificateEntry(String.format("%d", new Object[]{Integer.valueOf(index)}), cert);
                    index++;
                }
                if (index <= 0) {
                    Log.wtf(TAG, "No certs loaded");
                }
            } catch (Exception e) {
            }
        }
    }

    public KeyStore get() {
        return this.mKeyStore;
    }
}
