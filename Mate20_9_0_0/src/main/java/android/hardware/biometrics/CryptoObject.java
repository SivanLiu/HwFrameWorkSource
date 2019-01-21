package android.hardware.biometrics;

import android.security.keystore.AndroidKeyStoreProvider;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.Mac;

public class CryptoObject {
    private final Object mCrypto;

    public CryptoObject(Signature signature) {
        this.mCrypto = signature;
    }

    public CryptoObject(Cipher cipher) {
        this.mCrypto = cipher;
    }

    public CryptoObject(Mac mac) {
        this.mCrypto = mac;
    }

    public Signature getSignature() {
        return this.mCrypto instanceof Signature ? (Signature) this.mCrypto : null;
    }

    public Cipher getCipher() {
        return this.mCrypto instanceof Cipher ? (Cipher) this.mCrypto : null;
    }

    public Mac getMac() {
        return this.mCrypto instanceof Mac ? (Mac) this.mCrypto : null;
    }

    public final long getOpId() {
        return this.mCrypto != null ? AndroidKeyStoreProvider.getKeyStoreOperationHandle(this.mCrypto) : 0;
    }
}
