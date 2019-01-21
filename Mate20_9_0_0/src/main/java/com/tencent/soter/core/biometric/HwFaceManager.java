package com.tencent.soter.core.biometric;

import android.content.Context;
import android.os.CancellationSignal;
import android.os.Handler;
import com.huawei.hardware.face.FaceAuthenticationManager;
import com.huawei.hardware.face.FaceAuthenticationManager.AuthenticationResult;
import com.tencent.soter.core.biometric.FaceManager.AuthenticationCallback;
import com.tencent.soter.core.biometric.FaceManager.CryptoObject;
import java.security.Signature;
import javax.crypto.Cipher;
import javax.crypto.Mac;

public class HwFaceManager extends FaceManager {
    private static final int SOTER_FLAG = 5;
    private CryptoObject mCryptoObject;
    private FaceAuthenticationManager mFaceAuthenticationManager;

    public HwFaceManager(Context context) {
        this.mFaceAuthenticationManager = new FaceAuthenticationManager(context);
    }

    public String getBiometricName(Context context) {
        return "人脸";
    }

    public boolean hasEnrolledFaces() {
        return this.mFaceAuthenticationManager.hasEnrolledFace();
    }

    public boolean isHardwareDetected() {
        return this.mFaceAuthenticationManager.isHardwareDetected();
    }

    public void authenticate(CryptoObject crypto, CancellationSignal cancel, int flags, final AuthenticationCallback callback, Handler handler) {
        FaceAuthenticationManager.CryptoObject hwcrypto = null;
        if (crypto != null) {
            Signature signature = crypto.getSignature();
            Cipher cipher = crypto.getCipher();
            Mac mac = crypto.getMac();
            if (signature != null) {
                hwcrypto = new FaceAuthenticationManager.CryptoObject(signature);
            } else if (cipher != null) {
                hwcrypto = new FaceAuthenticationManager.CryptoObject(cipher);
            } else if (mac != null) {
                hwcrypto = new FaceAuthenticationManager.CryptoObject(mac);
            }
            this.mCryptoObject = crypto;
        }
        this.mFaceAuthenticationManager.authenticate(hwcrypto, cancel, 5, new FaceAuthenticationManager.AuthenticationCallback() {
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                callback.onAuthenticationError(errorCode, errString);
            }

            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                callback.onAuthenticationHelp(helpCode, helpString);
            }

            public void onAuthenticationSucceeded(AuthenticationResult result) {
                callback.onAuthenticationSucceeded(new FaceManager.AuthenticationResult(HwFaceManager.this.mCryptoObject));
            }

            public void onAuthenticationFailed() {
                callback.onAuthenticationFailed();
            }
        }, handler);
    }
}
