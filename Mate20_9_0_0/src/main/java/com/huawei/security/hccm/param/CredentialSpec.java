package com.huawei.security.hccm.param;

import android.support.annotation.NonNull;
import android.util.Log;
import com.huawei.security.hccm.CredentialException;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.util.Arrays;

public final class CredentialSpec {
    private static final String TAG = "CredentialSpec";
    public static final int TYPE_ALIAS = 1;
    public static final int TYPE_CHALLENGE = 3;
    public static final int TYPE_UNKNOWN = 0;
    public static final int TYPE_USERNAME = 2;
    private String mAlias;
    private byte[] mChallenge;
    private String mKeyStoretype;
    private byte[] mPassword;
    private int mType;
    private byte[] mUsername;

    public static final class Builder {
        private static final String TAG = "CredentialSpec.Builder";
        private String mAlias;
        private byte[] mAttestationChallenge;
        private String mKeyStoreType;
        private byte[] mPassword;
        private byte[] mUsername;

        public Builder(@NonNull byte[] challenge) {
            this.mUsername = null;
            this.mPassword = null;
            this.mAlias = null;
            this.mKeyStoreType = null;
            this.mAttestationChallenge = null;
            this.mAttestationChallenge = Arrays.copyOf(challenge, challenge.length);
        }

        public Builder(@NonNull String username, @NonNull String password) {
            this(username.getBytes(Charset.forName("UTF-8")), password.getBytes(Charset.forName("UTF-8")));
        }

        public Builder(@NonNull byte[] username, @NonNull byte[] password) {
            this.mUsername = null;
            this.mPassword = null;
            this.mAlias = null;
            this.mKeyStoreType = null;
            this.mAttestationChallenge = null;
            this.mUsername = Arrays.copyOf(username, username.length);
            this.mPassword = Arrays.copyOf(password, password.length);
        }

        /* JADX WARNING: Removed duplicated region for block: B:4:0x001f A:{Splitter: B:1:0x000e, ExcHandler: java.io.IOException (r0_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:4:0x001f A:{Splitter: B:1:0x000e, ExcHandler: java.io.IOException (r0_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:4:0x001f A:{Splitter: B:1:0x000e, ExcHandler: java.io.IOException (r0_2 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:4:0x001f, code:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:6:0x0029, code:
            throw new com.huawei.security.hccm.CredentialException(r0.getMessage(), r0);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public Builder(@NonNull Certificate user, @NonNull PrivateKey key) throws CredentialException {
            this.mUsername = null;
            this.mPassword = null;
            this.mAlias = null;
            this.mKeyStoreType = null;
            this.mAttestationChallenge = null;
            try {
                KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
                ks.load(null);
                this.mAlias = ks.getCertificateAlias(user);
            } catch (Exception e) {
            }
        }

        public Builder(@NonNull Certificate[] chain, @NonNull PrivateKey key) throws CredentialException {
            this(chain[0], key);
        }

        /* JADX WARNING: Removed duplicated region for block: B:4:0x0021 A:{Splitter: B:2:0x0011, ExcHandler: java.io.IOException (r0_1 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:4:0x0021 A:{Splitter: B:2:0x0011, ExcHandler: java.io.IOException (r0_1 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:4:0x0021 A:{Splitter: B:2:0x0011, ExcHandler: java.io.IOException (r0_1 'e' java.lang.Exception)} */
        /* JADX WARNING: Removed duplicated region for block: B:4:0x0021 A:{Splitter: B:2:0x0011, ExcHandler: java.io.IOException (r0_1 'e' java.lang.Exception)} */
        /* JADX WARNING: Missing block: B:4:0x0021, code:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:6:0x002b, code:
            throw new com.huawei.security.hccm.CredentialException(r0.getMessage(), r0);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public Builder(@NonNull int type, @NonNull String alias, @NonNull String keyStoreType) throws CredentialException {
            this.mUsername = null;
            this.mPassword = null;
            this.mAlias = null;
            this.mKeyStoreType = null;
            this.mAttestationChallenge = null;
            if (type == 1) {
                try {
                    KeyStore ks = KeyStore.getInstance(keyStoreType);
                    ks.load(null);
                    ks.getCertificateChain(alias);
                    ks.getKey(alias, null);
                    this.mAlias = alias;
                } catch (Exception e) {
                }
            }
        }

        public Builder setKeyStoreType(@NonNull String keyStoreType) {
            if (keyStoreType == null) {
                Log.e(TAG, "the keystore type is null");
                return null;
            }
            this.mKeyStoreType = keyStoreType;
            return this;
        }

        public CredentialSpec build() throws CredentialException {
            if (this.mAlias != null) {
                return new CredentialSpec(this.mAlias, this.mKeyStoreType, null);
            }
            if (this.mUsername != null && this.mPassword != null) {
                return new CredentialSpec(this.mUsername, this.mPassword, null);
            }
            if (this.mAttestationChallenge != null) {
                return new CredentialSpec(this.mAttestationChallenge, null);
            }
            throw new CredentialException("No credentials defined");
        }
    }

    private CredentialSpec(@NonNull String alias, @NonNull String keyStoreType) {
        this.mType = 0;
        this.mAlias = null;
        this.mKeyStoretype = null;
        this.mUsername = new byte[0];
        this.mPassword = new byte[0];
        this.mChallenge = new byte[0];
        this.mType = 1;
        this.mAlias = alias;
        this.mKeyStoretype = keyStoreType;
    }

    private CredentialSpec(@NonNull byte[] username, @NonNull byte[] password) {
        this.mType = 0;
        this.mAlias = null;
        this.mKeyStoretype = null;
        this.mUsername = new byte[0];
        this.mPassword = new byte[0];
        this.mChallenge = new byte[0];
        this.mType = 2;
        this.mUsername = Arrays.copyOf(username, username.length);
        this.mPassword = Arrays.copyOf(password, password.length);
    }

    private CredentialSpec(@NonNull byte[] challenge) {
        this.mType = 0;
        this.mAlias = null;
        this.mKeyStoretype = null;
        this.mUsername = new byte[0];
        this.mPassword = new byte[0];
        this.mChallenge = new byte[0];
        this.mType = 3;
        this.mChallenge = challenge;
    }

    public byte[] getChallenge() {
        byte[] challenge = new byte[null];
        if (this.mType == 3) {
            return Arrays.copyOf(this.mChallenge, this.mChallenge.length);
        }
        return challenge;
    }

    public String getAlias() {
        if (this.mType == 1) {
            return this.mAlias;
        }
        return null;
    }

    public String getKeyStoretype() {
        if (this.mType == 1) {
            return this.mKeyStoretype;
        }
        return null;
    }

    public byte[] getUsername() {
        byte[] userName = new byte[null];
        if (this.mType == 2) {
            return Arrays.copyOf(this.mUsername, this.mUsername.length);
        }
        return userName;
    }

    public byte[] getPassword() {
        byte[] password = new byte[null];
        if (this.mType == 2) {
            return Arrays.copyOf(this.mPassword, this.mPassword.length);
        }
        return password;
    }

    public int getType() {
        return this.mType;
    }
}
