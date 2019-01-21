package org.ifaa.android.manager.face;

import android.content.Context;
import android.util.Log;
import com.huawei.facerecognition.FaceRecognizeManager;
import com.huawei.facerecognition.FaceRecognizeManager.FaceRecognizeCallback;
import org.ifaa.android.manager.IFAAManagerV2;
import org.ifaa.android.manager.face.IFAAFaceManager.AuthenticatorCallback;

public class IFAAFaceRecognizeManager {
    public static final int CODE_CALLBACK_ACQUIRE = 3;
    public static final int CODE_CALLBACK_RESULT = 1;
    public static final int IFAA_FACE_AUTHENTICATOR_FAIL = 103;
    public static final int IFAA_FACE_AUTHENTICATOR_SUCCESS = 100;
    public static final int IFAA_FACE_AUTH_ERROR_CANCEL = 102;
    public static final int IFAA_FACE_AUTH_ERROR_LOCKED = 129;
    public static final int IFAA_FACE_AUTH_ERROR_TIMEOUT = 113;
    public static final int IFAA_FACE_AUTH_STATUS_BRIGHT = 406;
    public static final int IFAA_FACE_AUTH_STATUS_DARK = 405;
    public static final int IFAA_FACE_AUTH_STATUS_EYE_CLOSED = 403;
    public static final int IFAA_FACE_AUTH_STATUS_FACE_OFFET_BOTTOM = 412;
    public static final int IFAA_FACE_AUTH_STATUS_FACE_OFFET_LEFT = 409;
    public static final int IFAA_FACE_AUTH_STATUS_FACE_OFFET_RIGHT = 410;
    public static final int IFAA_FACE_AUTH_STATUS_FACE_OFFET_TOP = 411;
    public static final int IFAA_FACE_AUTH_STATUS_FAR_FACE = 404;
    public static final int IFAA_FACE_AUTH_STATUS_INSUFFICIENT = 402;
    public static final int IFAA_FACE_AUTH_STATUS_MOUTH_OCCLUSION = 408;
    public static final int IFAA_FACE_AUTH_STATUS_PARTIAL = 401;
    public static final int IFAA_FACE_AUTH_STATUS_QUALITY = 407;
    public static final String TAG = "IFAAFaceRecognize";
    public static final int TYPE_CALLBACK_AUTH = 2;
    private static IFAAFaceRecognizeManager mIFAAFrManager = null;
    private static FaceRecognizeManager mManager = null;
    private AuthenticatorCallback mAuthenticatorCallback = null;
    private FaceRecognizeCallback mFRCallback = new FaceRecognizeCallback() {
        public void onCallbackEvent(int reqId, int type, int code, int errorCode) {
            String str = IFAAFaceRecognizeManager.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("onCallbackEvent gotten reqId");
            stringBuilder.append(reqId);
            stringBuilder.append(" type ");
            stringBuilder.append(type);
            stringBuilder.append(" code ");
            stringBuilder.append(code);
            stringBuilder.append("errCode");
            stringBuilder.append(errorCode);
            Log.i(str, stringBuilder.toString());
            if (IFAAFaceRecognizeManager.this.mAuthenticatorCallback == null) {
                Log.e(IFAAFaceRecognizeManager.TAG, "mAuthenticatorCallback empty in onCallbackEvent ");
                IFAAFaceRecognizeManager.this.release();
                return;
            }
            if (type != 2) {
                str = IFAAFaceRecognizeManager.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("gotten not ifaa's auth callback reqid ");
                stringBuilder.append(reqId);
                stringBuilder.append(" type ");
                stringBuilder.append(type);
                stringBuilder.append(" code ");
                stringBuilder.append(code);
                stringBuilder.append("errCode");
                stringBuilder.append(errorCode);
                Log.e(str, stringBuilder.toString());
            } else if (code == 1) {
                int IFAAErrorCode = IFAAFaceRecognizeManager.converHwErrorCodeToIFAA(errorCode);
                String str2 = IFAAFaceRecognizeManager.TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("IFAAErrorCode");
                stringBuilder2.append(IFAAErrorCode);
                Log.i(str2, stringBuilder2.toString());
                if (IFAAErrorCode == 100) {
                    Log.i(IFAAFaceRecognizeManager.TAG, "ifaa face auth success");
                    IFAAFaceRecognizeManager.this.mAuthenticatorCallback.onAuthenticationSucceeded();
                } else if (IFAAErrorCode == IFAAFaceRecognizeManager.IFAA_FACE_AUTH_ERROR_CANCEL || IFAAErrorCode == IFAAFaceRecognizeManager.IFAA_FACE_AUTH_ERROR_TIMEOUT || IFAAErrorCode == IFAAFaceRecognizeManager.IFAA_FACE_AUTH_ERROR_LOCKED) {
                    IFAAFaceRecognizeManager.this.mAuthenticatorCallback.onAuthenticationError(IFAAErrorCode);
                } else {
                    IFAAFaceRecognizeManager.this.mAuthenticatorCallback.onAuthenticationFailed(IFAAErrorCode);
                    str2 = IFAAFaceRecognizeManager.TAG;
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("fail reason");
                    stringBuilder2.append(IFAAErrorCode);
                    Log.e(str2, stringBuilder2.toString());
                }
                IFAAFaceRecognizeManager.this.release();
            } else if (code == 3) {
                IFAAFaceRecognizeManager.this.mAuthenticatorCallback.onAuthenticationStatus(IFAAFaceRecognizeManager.converHwAcquireInfoToIFAA(errorCode));
            } else {
                Log.e(IFAAFaceRecognizeManager.TAG, "bad err code,ignore");
            }
        }
    };

    public static int converHwAcquireInfoToIFAA(int hwAcquireInfo) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("converHwhwAcquireInfoToIFAA hwAcquireInfo is");
        stringBuilder.append(hwAcquireInfo);
        Log.e(str, stringBuilder.toString());
        if (hwAcquireInfo == 0) {
            return 100;
        }
        if (hwAcquireInfo == 22) {
            return IFAA_FACE_AUTH_STATUS_EYE_CLOSED;
        }
        switch (hwAcquireInfo) {
            case IFAAManagerV2.IFAA_AUTH_FACE /*4*/:
                return IFAA_FACE_AUTH_STATUS_QUALITY;
            case 5:
            case 6:
                return IFAA_FACE_AUTH_STATUS_INSUFFICIENT;
            case 7:
                return IFAA_FACE_AUTH_STATUS_FAR_FACE;
            case IFAAManagerV2.IFAA_AUTH_PIN /*8*/:
                return IFAA_FACE_AUTH_STATUS_FACE_OFFET_LEFT;
            case 9:
                return IFAA_FACE_AUTH_STATUS_FACE_OFFET_TOP;
            case 10:
                return IFAA_FACE_AUTH_STATUS_FACE_OFFET_RIGHT;
            case 11:
                return IFAA_FACE_AUTH_STATUS_FACE_OFFET_BOTTOM;
            default:
                switch (hwAcquireInfo) {
                    case 29:
                        return IFAA_FACE_AUTH_STATUS_PARTIAL;
                    case 30:
                        return IFAA_FACE_AUTH_STATUS_DARK;
                    case 31:
                        return IFAA_FACE_AUTH_STATUS_BRIGHT;
                    default:
                        return IFAA_FACE_AUTHENTICATOR_FAIL;
                }
        }
    }

    public static int converHwErrorCodeToIFAA(int hwErrorCode) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("converHwErrorCodeToIFAA hwErrorCode is");
        stringBuilder.append(hwErrorCode);
        Log.e(str, stringBuilder.toString());
        if (hwErrorCode == 0) {
            return 100;
        }
        if (hwErrorCode == 2) {
            return IFAA_FACE_AUTH_ERROR_CANCEL;
        }
        if (hwErrorCode == 4) {
            return IFAA_FACE_AUTH_ERROR_TIMEOUT;
        }
        if (hwErrorCode != 8) {
            return IFAA_FACE_AUTHENTICATOR_FAIL;
        }
        return IFAA_FACE_AUTH_ERROR_LOCKED;
    }

    public static synchronized void createInstance(Context context) {
        synchronized (IFAAFaceRecognizeManager.class) {
            if (mIFAAFrManager == null) {
                mIFAAFrManager = new IFAAFaceRecognizeManager(context);
            }
        }
    }

    public static IFAAFaceRecognizeManager getInstance() {
        return mIFAAFrManager;
    }

    public IFAAFaceRecognizeManager(Context context) {
        if (mManager == null) {
            mManager = new FaceRecognizeManager(context, this.mFRCallback);
        }
    }

    public static FaceRecognizeManager getFRManager() {
        return mManager;
    }

    public int init() {
        if (mManager != null) {
            return mManager.init();
        }
        return -1;
    }

    public void release() {
        if (mManager != null) {
            mManager.release();
        }
    }

    public void setAuthCallback(AuthenticatorCallback authCallback) {
        this.mAuthenticatorCallback = authCallback;
    }
}
