package android.trustcircle;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CancellationSignal.OnCancelListener;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.trustcircle.AuthPara.InitAuthInfo;
import android.trustcircle.AuthPara.OnAuthAckInfo;
import android.trustcircle.AuthPara.OnAuthSyncAckInfo;
import android.trustcircle.AuthPara.OnAuthSyncInfo;
import android.trustcircle.AuthPara.RecAckInfo;
import android.trustcircle.AuthPara.RecAuthAckInfo;
import android.trustcircle.AuthPara.RecAuthInfo;
import android.trustcircle.AuthPara.ReqPkInfo;
import android.trustcircle.AuthPara.RespPkInfo;
import android.trustcircle.AuthPara.Type;
import android.util.Log;
import huawei.android.security.IAuthCallback;
import huawei.android.security.IHwSecurityService;
import huawei.android.security.IKaCallback;
import huawei.android.security.ILifeCycleCallback;
import huawei.android.security.ILifeCycleCallback.Stub;
import huawei.android.security.ITrustCircleManager;

public class TrustCircleManager {
    private static final int AUTH_ID_ERROR = -1;
    private static final int LIFE_CYCLE_ERROR = -1;
    private static final int RESULT_OK = 0;
    private static final String SECURITY_SERVICE = "securityserver";
    private static final String TAG = "TrustCircleManager";
    private static final int TRUSTCIRCLE_PLUGIN_ID = 5;
    private static volatile TrustCircleManager sInstance;
    private ILifeCycleCallback mILifeCycleCallback = new Stub() {
        public void onRegisterResponse(int errorCode, int globalKeyID, int authKeyAlgoType, String regAuthKeyData, String regAuthKeyDataSign, String clientChallenge) {
            if (TrustCircleManager.this.mLoginCallback != null) {
                TrustCircleManager.this.mLoginCallback.onRegisterResponse(errorCode, globalKeyID, (short) authKeyAlgoType, regAuthKeyData, regAuthKeyDataSign, clientChallenge);
                if (errorCode != 0) {
                    TrustCircleManager.this.mLoginCallback = null;
                }
            }
        }

        public void onFinalRegisterResult(int errorCode) {
            if (TrustCircleManager.this.mLoginCallback != null) {
                TrustCircleManager.this.mLoginCallback.onFinalRegisterResult(errorCode);
                if (errorCode != 0) {
                    TrustCircleManager.this.mLoginCallback = null;
                }
            }
        }

        public void onLoginResponse(int errorCode, int indexVersion, String clientChallenge) {
            if (TrustCircleManager.this.mLoginCallback != null) {
                TrustCircleManager.this.mLoginCallback.onLoginResponse(errorCode, indexVersion, clientChallenge);
                if (errorCode != 0) {
                    TrustCircleManager.this.mLoginCallback = null;
                }
            }
        }

        public void onUpdateResponse(int errorCode, int indexVersion, String clientChallenge) {
            if (TrustCircleManager.this.mLoginCallback != null) {
                TrustCircleManager.this.mLoginCallback.onUpdateResponse(errorCode, indexVersion, clientChallenge);
                if (errorCode != 0) {
                    TrustCircleManager.this.mLoginCallback = null;
                }
            }
        }

        public void onFinalLoginResult(int errorCode) {
            if (TrustCircleManager.this.mLoginCallback != null) {
                TrustCircleManager.this.mLoginCallback.onFinalLoginResult(errorCode);
                TrustCircleManager.this.mLoginCallback = null;
            }
        }

        public void onLogoutResult(int errorCode) {
            if (TrustCircleManager.this.mLogoutCallback != null) {
                TrustCircleManager.this.mLogoutCallback.onLogoutResult(errorCode);
                TrustCircleManager.this.mLogoutCallback = null;
            }
        }

        public void onUnregisterResult(int errorCode) {
            if (TrustCircleManager.this.mUnregisterCallback != null) {
                TrustCircleManager.this.mUnregisterCallback.onUnregisterResult(errorCode);
                TrustCircleManager.this.mUnregisterCallback = null;
            }
        }
    };
    private LoginCallback mLoginCallback;
    private LogoutCallback mLogoutCallback;
    private IHwSecurityService mSecurityService = IHwSecurityService.Stub.asInterface(ServiceManager.getService(SECURITY_SERVICE));
    private UnregisterCallback mUnregisterCallback;

    public interface AuthCallback {
        void onAuthAck(long j, OnAuthAckInfo onAuthAckInfo);

        void onAuthAckError(long j, int i);

        void onAuthError(long j, int i);

        void onAuthExited(long j, int i);

        void onAuthSync(long j, OnAuthSyncInfo onAuthSyncInfo);

        void onAuthSyncAck(long j, OnAuthSyncAckInfo onAuthSyncAckInfo);

        void onAuthSyncAckError(long j, int i);

        void requestPK();

        void responsePK(long j, RespPkInfo respPkInfo);
    }

    private static class AuthCallbackInner extends IAuthCallback.Stub {
        AuthCallback mCallback;

        AuthCallbackInner(AuthCallback callback) {
            this.mCallback = callback;
        }

        public void onAuthSync(long authID, byte[] tcisId, int pkVersion, int taVersion, long nonce, int authKeyAlgoType, byte[] authKeyInfo, byte[] authKeyInfoSign) {
            if (this.mCallback != null) {
                this.mCallback.onAuthSync(authID, new OnAuthSyncInfo(tcisId, pkVersion, (short) taVersion, nonce, (short) authKeyAlgoType, authKeyInfo, authKeyInfoSign));
                return;
            }
            long j = authID;
            int i = taVersion;
            int i2 = authKeyAlgoType;
        }

        public void onAuthError(long authID, int errorCode) {
            if (this.mCallback != null) {
                this.mCallback.onAuthError(authID, errorCode);
            }
        }

        public void onAuthSyncAck(long authID, byte[] tcisIdSlave, int pkVersionSlave, long nonceSlave, byte[] mac, int authKeyAlgoType, byte[] authKeyInfo, byte[] authKeyInfoSign) {
            if (this.mCallback != null) {
                this.mCallback.onAuthSyncAck(authID, new OnAuthSyncAckInfo(tcisIdSlave, pkVersionSlave, nonceSlave, mac, (short) authKeyAlgoType, authKeyInfo, authKeyInfoSign));
                return;
            }
            long j = authID;
            int i = authKeyAlgoType;
        }

        public void onAuthSyncAckError(long authID, int errorCode) {
            if (this.mCallback != null) {
                this.mCallback.onAuthSyncAckError(authID, errorCode);
            }
        }

        public void onAuthAck(long authID, int result, byte[] sessionKeyIV, byte[] sessionKey, byte[] mac) {
            if (this.mCallback != null) {
                this.mCallback.onAuthAck(authID, new OnAuthAckInfo(result, sessionKeyIV, sessionKey, mac));
            }
        }

        public void onAuthAckError(long authID, int errorCode) {
            if (this.mCallback != null) {
                this.mCallback.onAuthAckError(authID, errorCode);
            }
        }

        public void requestPK() {
            if (this.mCallback != null) {
                this.mCallback.requestPK();
            }
        }

        public void responsePK(long authID, int authKeyAlgoType, byte[] authKeyData, byte[] authKeyDataSign) {
            if (this.mCallback != null) {
                this.mCallback.responsePK(authID, new RespPkInfo((short) authKeyAlgoType, authKeyData, authKeyDataSign));
            }
        }

        public void onAuthExited(long authID, int resultCode) {
            if (this.mCallback != null) {
                this.mCallback.onAuthExited(authID, resultCode);
            }
        }
    }

    private static class IKaCallbackInner extends IKaCallback.Stub {
        KaCallback mKaCallback;

        public IKaCallbackInner(KaCallback callback) {
            this.mKaCallback = callback;
        }

        public void onKaResult(long authId, int result, byte[] iv, byte[] payload) {
            if (this.mKaCallback != null) {
                this.mKaCallback.onKaResult(authId, result, iv, payload);
                this.mKaCallback = null;
            }
        }

        public void onKaError(long authId, int errorCode) {
            if (this.mKaCallback != null) {
                this.mKaCallback.onKaError(authId, errorCode);
                this.mKaCallback = null;
            }
        }
    }

    public interface KaCallback {
        void onKaError(long j, int i);

        void onKaResult(long j, int i, byte[] bArr, byte[] bArr2);
    }

    public interface LoginCallback {
        void onFinalLoginResult(int i);

        void onFinalRegisterResult(int i);

        void onLoginResponse(int i, int i2, String str);

        void onRegisterResponse(int i, int i2, short s, String str, String str2, String str3);

        void onUpdateResponse(int i, int i2, String str);
    }

    public interface LogoutCallback {
        void onLogoutResult(int i);
    }

    private class OnAuthenticationCancelListener implements OnCancelListener {
        private long authID;

        OnAuthenticationCancelListener(long authID) {
            this.authID = authID;
        }

        public void onCancel() {
            TrustCircleManager.this.cancelAuthentication(this.authID);
        }
    }

    private class OnRegOrLoginCancelListener implements OnCancelListener {
        private long userID;

        public OnRegOrLoginCancelListener(long userID) {
            this.userID = userID;
        }

        public void onCancel() {
            Log.d(TrustCircleManager.TAG, "cancelRegOrLogin");
            TrustCircleManager.this.cancelRegOrUpdate(this.userID);
        }
    }

    public interface UnregisterCallback {
        void onUnregisterResult(int i);
    }

    private TrustCircleManager() {
        if (this.mSecurityService == null) {
            Log.e(TAG, "error, securityservice was null");
        }
    }

    public static TrustCircleManager getInstance() {
        if (sInstance == null) {
            synchronized (TrustCircleManager.class) {
                if (sInstance == null) {
                    sInstance = new TrustCircleManager();
                }
            }
        }
        return sInstance;
    }

    private ITrustCircleManager getTrustCirclePlugin() {
        synchronized (this) {
            if (this.mSecurityService != null) {
                try {
                    ITrustCircleManager tcisService = ITrustCircleManager.Stub.asInterface(this.mSecurityService.querySecurityInterface(5));
                    if (tcisService == null) {
                        Log.e(TAG, "error, TrustCirclePlugin is null");
                    }
                    return tcisService;
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException when getTrustCirclePlugin invoked");
                }
            }
            Log.e(TAG, "error, SecurityService is null");
            return null;
        }
    }

    public Bundle getTcisInfo() {
        ITrustCircleManager plugin = getTrustCirclePlugin();
        if (plugin != null) {
            try {
                return plugin.getTcisInfo();
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when getTcisInfo is invoked");
            }
        }
        return null;
    }

    public long initKeyAgreement(KaCallback callback, int kaVersion, long userId, byte[] aesTmpKey, String kaInfo) {
        KaCallback kaCallback = callback;
        if (kaCallback == null || aesTmpKey == null || kaInfo == null) {
            throw new IllegalArgumentException("illegal null params.");
        }
        ITrustCircleManager plugin = getTrustCirclePlugin();
        if (plugin != null) {
            IKaCallbackInner mKaCallback = new IKaCallbackInner(kaCallback);
            try {
                return plugin.initKeyAgreement(mKaCallback, kaVersion, userId, aesTmpKey, kaInfo);
            } catch (RemoteException e) {
                RemoteException remoteException = e;
                Log.e(TAG, "RemoteException when initKeyAgreement is invoked");
                mKaCallback.onKaError(-1, -1);
            }
        }
        return -1;
    }

    public void loginServerRequest(LoginCallback callback, CancellationSignal cancel, long userID, int serverRegisterStatus, String sessionID) {
        LoginCallback loginCallback = callback;
        CancellationSignal cancellationSignal = cancel;
        long j;
        if (loginCallback == null) {
            j = userID;
            throw new IllegalArgumentException("Must supply an login callback");
        } else if (sessionID == null) {
            Log.e(TAG, "session id should't be null");
            loginCallback.onLoginResponse(-1, -1, null);
        } else {
            if (cancellationSignal == null) {
                j = userID;
            } else if (cancel.isCanceled()) {
                Log.e(TAG, "login already canceled");
                return;
            } else {
                j = userID;
                cancellationSignal.setOnCancelListener(new OnRegOrLoginCancelListener(j));
            }
            ITrustCircleManager plugin = getTrustCirclePlugin();
            if (plugin != null) {
                this.mLoginCallback = loginCallback;
                try {
                    plugin.loginServerRequest(this.mILifeCycleCallback, j, serverRegisterStatus, sessionID);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException when loginServerRequest is invoked");
                    if (this.mLoginCallback != null) {
                        this.mLoginCallback.onRegisterResponse(-1, -1, (short) -1, null, null, null);
                        this.mLoginCallback = null;
                    }
                }
            }
        }
    }

    public void updateServerRequest(LoginCallback callback, CancellationSignal cancel, long userID) {
        if (callback != null) {
            if (cancel != null) {
                if (cancel.isCanceled()) {
                    Log.e(TAG, "update already canceled");
                    return;
                }
                cancel.setOnCancelListener(new OnRegOrLoginCancelListener(userID));
            }
            ITrustCircleManager plugin = getTrustCirclePlugin();
            if (plugin != null) {
                this.mLoginCallback = callback;
                try {
                    plugin.updateServerRequest(this.mILifeCycleCallback, userID);
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException when loginServerRequest is invoked");
                    if (this.mLoginCallback != null) {
                        this.mLoginCallback.onUpdateResponse(-1, -1, null);
                        this.mLoginCallback = null;
                    }
                }
            }
            return;
        }
        throw new IllegalArgumentException("Must supply an login callback");
    }

    public void finalRegister(String authPKData, String authPKDataSign, String updateIndexInfo, String updateIndexSignature) {
        if (authPKData == null || authPKDataSign == null) {
            StringBuffer exception = new StringBuffer();
            String str = authPKData == null ? "authPKData " : authPKDataSign == null ? "and " : "";
            exception.append(str);
            exception.append(authPKDataSign == null ? "authPKDataSign " : "");
            exception.append("should't be null");
            Log.e(TAG, exception.toString());
            return;
        }
        ITrustCircleManager plugin = getTrustCirclePlugin();
        if (plugin != null) {
            try {
                plugin.finalRegister(this.mILifeCycleCallback, authPKData, authPKDataSign, updateIndexInfo, updateIndexSignature);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when finalRegister is invoked");
                if (this.mLoginCallback != null) {
                    this.mLoginCallback.onFinalRegisterResult(-1);
                    this.mLoginCallback = null;
                }
            }
        }
    }

    public void finalLogin(int updateResult, String updateIndexInfo, String updateIndexSignature) {
        ITrustCircleManager plugin = getTrustCirclePlugin();
        if (plugin != null) {
            try {
                plugin.finalLogin(this.mILifeCycleCallback, updateResult, updateIndexInfo, updateIndexSignature);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when finalLogin is invoked");
                if (this.mLoginCallback != null) {
                    this.mLoginCallback.onFinalLoginResult(-1);
                    this.mLoginCallback = null;
                }
            }
        }
    }

    public void logout(LogoutCallback callback, long userID) {
        if (callback != null) {
            ITrustCircleManager plugin = getTrustCirclePlugin();
            if (plugin != null) {
                try {
                    this.mLogoutCallback = callback;
                    plugin.logout(this.mILifeCycleCallback, userID);
                    return;
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException when logout is invoked");
                    if (this.mLogoutCallback != null) {
                        this.mLogoutCallback.onLogoutResult(-1);
                        this.mLogoutCallback = null;
                        return;
                    }
                    return;
                }
            }
            return;
        }
        throw new IllegalArgumentException("Must supply an logout callback");
    }

    private void cancelRegOrUpdate(long userID) {
        ITrustCircleManager plugin = getTrustCirclePlugin();
        if (plugin != null) {
            try {
                plugin.cancelRegOrLogin(this.mILifeCycleCallback, userID);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException when cancelRegOrUpdate is invoked");
            }
        }
    }

    public void unregister(UnregisterCallback callback, long userID) {
        if (callback != null) {
            ITrustCircleManager plugin = getTrustCirclePlugin();
            if (plugin != null) {
                try {
                    this.mUnregisterCallback = callback;
                    plugin.unregister(this.mILifeCycleCallback, userID);
                    return;
                } catch (RemoteException e) {
                    Log.e(TAG, "RemoteException when unregister is invoked");
                    if (this.mUnregisterCallback != null) {
                        this.mUnregisterCallback.onUnregisterResult(-1);
                        this.mUnregisterCallback = null;
                        return;
                    }
                    return;
                }
            }
            return;
        }
        throw new IllegalArgumentException("Must supply an unregister callback");
    }

    public long activeAuth(AuthCallback callback, CancellationSignal cancel, InitAuthInfo info) {
        ITrustCircleManager plugin = getTrustCirclePlugin();
        long authID = -1;
        if (!(plugin == null || info == null || callback == null)) {
            try {
                authID = plugin.initAuthenticate(new AuthCallbackInner(callback), info.mAuthType, info.mAuthVersion, info.mPolicy, info.mUserID, info.mAESTmpKey);
            } catch (RemoteException e) {
                Log.e(TAG, "initAuthenticate failed");
            }
        }
        if (!(cancel == null || cancel.isCanceled() || authID == -1)) {
            cancel.setOnCancelListener(new OnAuthenticationCancelListener(authID));
        }
        return authID;
    }

    public long passiveAuth(AuthCallback callback, CancellationSignal cancel, RecAuthInfo info) {
        long j;
        AuthCallback authCallback = callback;
        CancellationSignal cancellationSignal = cancel;
        RecAuthInfo recAuthInfo = info;
        ITrustCircleManager plugin = getTrustCirclePlugin();
        if (!(plugin == null || recAuthInfo == null || authCallback == null)) {
            try {
                AuthCallbackInner authCallbackInner = new AuthCallbackInner(authCallback);
                int i = recAuthInfo.mAuthType;
                int i2 = recAuthInfo.mAuthVersion;
                short s = recAuthInfo.mTAVersion;
                int i3 = recAuthInfo.mPolicy;
                long j2 = recAuthInfo.mUserID;
                byte[] bArr = recAuthInfo.mAESTmpKey;
                byte[] bArr2 = recAuthInfo.mTcisId;
                int i4 = recAuthInfo.mPkVersion;
                j = recAuthInfo.mNonce;
                short s2 = recAuthInfo.mAuthKeyAlgoType;
                short s3 = s2;
                j = plugin.receiveAuthSync(authCallbackInner, i, i2, s, i3, j2, bArr, bArr2, i4, j, s3, recAuthInfo.mAuthKeyInfo, recAuthInfo.mAuthKeyInfoSign);
            } catch (RemoteException e) {
                Log.e(TAG, "passiveAuth failed");
            }
            cancellationSignal = cancel;
            if (cancellationSignal != null || cancel.isCanceled() || j == -1) {
            } else {
                cancellationSignal.setOnCancelListener(new OnAuthenticationCancelListener(j));
            }
            return j;
        }
        j = -1;
        cancellationSignal = cancel;
        if (cancellationSignal != null) {
        }
        return j;
    }

    /* JADX WARNING: Missing block: B:9:0x0021, code skipped:
            r17 = r12;
     */
    /* JADX WARNING: Missing block: B:23:0x0054, code skipped:
            r17 = r12;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean receiveAuthInfo(Type type, long authID, Object info) {
        String str;
        StringBuilder stringBuilder;
        Type type2 = type;
        long j = authID;
        ReqPkInfo reqPkInfo = info;
        ITrustCircleManager plugin = getTrustCirclePlugin();
        if (plugin == null || type2 == null || reqPkInfo == null || j < 0) {
        } else {
            ITrustCircleManager iTrustCircleManager;
            try {
                switch (type) {
                    case REC_AUTH_SYNC_ACK:
                        if (reqPkInfo instanceof RecAuthAckInfo) {
                            RecAuthAckInfo authAckInfo = (RecAuthAckInfo) reqPkInfo;
                            iTrustCircleManager = plugin;
                            try {
                                return plugin.receiveAuthSyncAck(j, authAckInfo.mTcisIDSlave, authAckInfo.mPkVersionSlave, authAckInfo.mNonceSlave, authAckInfo.mMacSlave, authAckInfo.mAuthKeyAlgoTypeSlave, authAckInfo.mAuthKeyInfoSlave, authAckInfo.mAuthKeyInfoSignSlave);
                            } catch (RemoteException e) {
                                str = TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("reveiveAuthInfo, type: ");
                                stringBuilder.append(type2);
                                stringBuilder.append(" failed");
                                Log.e(str, stringBuilder.toString());
                                return false;
                            }
                        }
                        break;
                    case REC_ACK:
                        if (reqPkInfo instanceof RecAckInfo) {
                            return plugin.receiveAck(j, ((RecAckInfo) reqPkInfo).mMAC);
                        }
                        break;
                    case REC_PK:
                        if (reqPkInfo instanceof RespPkInfo) {
                            RespPkInfo pkInfo = (RespPkInfo) reqPkInfo;
                            return plugin.receivePK(j, pkInfo.mAuthKeyAlgoType, pkInfo.mAuthKeyData, pkInfo.mAuthKeyDataSign);
                        }
                        break;
                    case REQ_PK:
                        try {
                            if (reqPkInfo instanceof ReqPkInfo) {
                                return plugin.requestPK(j, reqPkInfo.mUserID);
                            }
                        } catch (RemoteException e2) {
                            iTrustCircleManager = plugin;
                            str = TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("reveiveAuthInfo, type: ");
                            stringBuilder.append(type2);
                            stringBuilder.append(" failed");
                            Log.e(str, stringBuilder.toString());
                            return false;
                        }
                        break;
                }
            } catch (RemoteException e3) {
                iTrustCircleManager = plugin;
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("reveiveAuthInfo, type: ");
                stringBuilder.append(type2);
                stringBuilder.append(" failed");
                Log.e(str, stringBuilder.toString());
                return false;
            }
        }
        return false;
    }

    private void cancelAuthentication(long authId) {
        ITrustCircleManager plugin = getTrustCirclePlugin();
        if (plugin != null) {
            try {
                plugin.cancelAuthentication(authId);
            } catch (RemoteException e) {
                Log.e(TAG, "cancelAuthentication failed");
            }
        }
    }
}
