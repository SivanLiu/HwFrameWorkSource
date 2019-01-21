package huawei.android.security.voicerecognition;

import android.content.Context;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.IBinder.DeathRecipient;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Slog;
import huawei.android.security.voicerecognition.IHeadsetStatusCallback.Stub;
import java.util.List;

public class VoiceRecognizeManager implements DeathRecipient {
    public static final int MSG_AUTH_FAILED = 6;
    public static final int MSG_AUTH_SUCCESS = 5;
    public static final int MSG_ENROLL_ACQUIRE = 2;
    public static final int MSG_ENROLL_CANCEL_RET = 3;
    public static final int MSG_ENROLL_RESULT = 1;
    public static final int MSG_HEADSET_STATUS_CHANGE = 7;
    public static final int MSG_HEADSET_STATUS_CHANGE_TO_CLIENT = 8;
    public static final int MSG_REMOVE_RET = 4;
    private static final String TAG = "VoiceRecognizeManager";
    public static final int VOICEID_ENROLL_BUSY = 1;
    public static final int VOICEID_ENROLL_FAILED = 4;
    public static final int VOICEID_ENROLL_NOT_CONNECT = 2;
    public static final int VOICEID_ENROLL_OK = 0;
    public static final int VOICEID_ENROLL_TIMEOUT = 3;
    public static final int VOICEID_TYPE_AUTH_RET = 5;
    public static final int VOICEID_TYPE_ENROLL_ACQUIRE = 2;
    public static final int VOICEID_TYPE_ENROLL_CANCEL_RET = 3;
    public static final int VOICEID_TYPE_ENROLL_RET = 1;
    public static final int VOICEID_TYPE_REMOVE_RET = 4;
    public static final String VOICERECOGNIZE_SERVICE = "voicerecognition";
    private AuthCallback mAuthCallback = null;
    private Context mContext;
    private EnrollmentCallback mEnrollCallback;
    private Handler mHandler;
    private IHeadsetStatusCallback mHeadsetStatusCallback = new Stub() {
        public void onHeadsetStatusChange(int status) {
            if (VoiceRecognizeManager.this.mHandler != null) {
                VoiceRecognizeManager.this.mHandler.obtainMessage(8, status, 0).sendToTarget();
            } else {
                VoiceRecognizeManager.this.sendHeadsetStatusChangedToClient(status);
            }
        }
    };
    private HeadsetStatusCallback mHeadsetStatusCb;
    private RemoveCallback mRemoveCallback;
    private IVoiceRecognizeService mService = null;
    private IBinder mServiceBinder = null;
    private IVoiceRecognizeServiceReceiver mServiceReceiver = new IVoiceRecognizeServiceReceiver.Stub() {
        public void onOptCallback(int type, int code, int subCode1, int subCode2) {
            switch (type) {
                case 1:
                    if (VoiceRecognizeManager.this.mHandler != null) {
                        VoiceRecognizeManager.this.mHandler.obtainMessage(1, code, subCode1, null).sendToTarget();
                        return;
                    } else {
                        VoiceRecognizeManager.this.sendEnrollResult(code, subCode1);
                        return;
                    }
                case 2:
                    if (VoiceRecognizeManager.this.mHandler != null) {
                        VoiceRecognizeManager.this.mHandler.obtainMessage(2, code, subCode1, null).sendToTarget();
                        return;
                    } else {
                        VoiceRecognizeManager.this.sendEnrollAcquire(code, subCode1);
                        return;
                    }
                case 3:
                    if (VoiceRecognizeManager.this.mHandler != null) {
                        VoiceRecognizeManager.this.mHandler.obtainMessage(3, code, 0, null).sendToTarget();
                        return;
                    } else {
                        VoiceRecognizeManager.this.sendRemoveResult(code);
                        return;
                    }
                case 4:
                    if (VoiceRecognizeManager.this.mHandler != null) {
                        VoiceRecognizeManager.this.mHandler.obtainMessage(4, code, 0, null).sendToTarget();
                        return;
                    } else {
                        VoiceRecognizeManager.this.sendRemoveResult(code);
                        return;
                    }
                default:
                    return;
            }
        }
    };
    private IBinder mToken = new Binder();
    private IVoiceAuthCallback mVoiceAuthCallback = new IVoiceAuthCallback.Stub() {
        public void onReceiveAuthVoice(int userId, int type) {
            if (VoiceRecognizeManager.this.mHandler != null) {
                VoiceRecognizeManager.this.mHandler.obtainMessage(5, userId, type, null).sendToTarget();
            } else {
                VoiceRecognizeManager.this.sendAuthSuccess(userId, type);
            }
        }

        public void onReceiveUnAuthVoice(int userId, int type) {
            if (VoiceRecognizeManager.this.mHandler != null) {
                VoiceRecognizeManager.this.mHandler.obtainMessage(6, userId, type).sendToTarget();
            } else {
                VoiceRecognizeManager.this.sendAuthFailed(userId, type);
            }
        }

        public void onHeadsetStatusChange(int status) {
            if (VoiceRecognizeManager.this.mHandler != null) {
                VoiceRecognizeManager.this.mHandler.obtainMessage(7, status, 0, null).sendToTarget();
            } else {
                VoiceRecognizeManager.this.sendHeadsetStatusChange(status);
            }
        }
    };

    public static abstract class AuthCallback {
        public boolean onReceiveAuthVoice(int userId, int type) {
            return false;
        }

        public void onReceiveUnAuthVoice(int userId, int type, int totalFailedTimes) {
        }

        public void onHeadsetStatusChange(int status) {
        }
    }

    public static abstract class EnrollmentCallback {
        public void onEnrollResult(int ret, int id) {
        }

        public void onEnrollAcquire(int acquireInfo, int subInfo) {
        }

        public void onEnrollCancelResult(int ret) {
        }
    }

    public static abstract class HeadsetStatusCallback {
        public void onHeadsetStatusChange(int status) {
        }
    }

    private class MyHandler extends Handler {
        /* synthetic */ MyHandler(VoiceRecognizeManager x0, Context x1, AnonymousClass1 x2) {
            this(x1);
        }

        private MyHandler(Context context) {
            super(context.getMainLooper());
        }

        private MyHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    VoiceRecognizeManager.this.sendEnrollResult(msg.arg1, msg.arg2);
                    return;
                case 2:
                    VoiceRecognizeManager.this.sendEnrollAcquire(msg.arg1, msg.arg2);
                    return;
                case 3:
                    VoiceRecognizeManager.this.sendEnrollCancelResult(msg.arg1);
                    return;
                case 4:
                    VoiceRecognizeManager.this.sendRemoveResult(msg.arg1);
                    return;
                case 5:
                    VoiceRecognizeManager.this.sendAuthSuccess(msg.arg1, msg.arg2);
                    return;
                case 6:
                    VoiceRecognizeManager.this.sendAuthFailed(msg.arg1, msg.arg2);
                    return;
                case 7:
                    VoiceRecognizeManager.this.sendHeadsetStatusChange(msg.arg1);
                    return;
                case 8:
                    VoiceRecognizeManager.this.sendHeadsetStatusChangedToClient(msg.arg1);
                    return;
                default:
                    return;
            }
        }
    }

    public static abstract class RemoveCallback {
        public void onRemoveResult(int ret) {
        }
    }

    public VoiceRecognizeManager(Context context) {
        this.mContext = context;
        if (context != null) {
            this.mHandler = new MyHandler(this, context, null);
        } else {
            this.mHandler = null;
        }
    }

    public void setAuthCallback(AuthCallback authCallback) {
        this.mService = getService();
        if (this.mService != null) {
            try {
                this.mAuthCallback = authCallback;
                this.mService.setAuthCallback(this.mVoiceAuthCallback);
                return;
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("set authcallback failed, because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
                return;
            }
        }
        Slog.e(TAG, "setAuthCallback failed because get service failed");
    }

    public boolean setHeadsetStatusCallback(HeadsetStatusCallback headsetStatusCallback) {
        this.mService = getService();
        if (this.mService != null) {
            try {
                String opPackageName = "";
                this.mService.setHeadsetStatusCallback(this.mHeadsetStatusCallback);
                this.mHeadsetStatusCb = headsetStatusCallback;
                return true;
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setHeadsetStatusCallback failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "setHeadsetStatusCallback failed because get service failed");
            return false;
        }
    }

    public boolean enroll(byte[] authToken, int flags, EnrollmentCallback callback) {
        if (authToken == null || callback == null) {
            Slog.e(TAG, "enroll failed because invalid parameter");
            return false;
        }
        this.mService = getService();
        if (this.mService != null) {
            try {
                byte[] bArr = authToken;
                int i = flags;
                this.mService.enroll(this.mToken, bArr, i, 0, this.mServiceReceiver, "");
                this.mEnrollCallback = callback;
                return true;
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("enroll failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "enroll failed because get service failed");
            return false;
        }
    }

    public boolean cancelEnroll() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                this.mService.cancelEnroll(this.mToken);
                return true;
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("cancel enroll failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "cancel enroll failed because get service failed");
            return false;
        }
    }

    public boolean continueEnroll() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                this.mService.continueEnroll();
                return true;
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("continueEnroll failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "continueEnroll failed because get service failed");
            return false;
        }
    }

    public boolean remove(int voiceId, RemoveCallback callback) {
        if (callback == null) {
            return false;
        }
        this.mService = getService();
        if (this.mService != null) {
            try {
                this.mService.remove(this.mToken, voiceId, 0, this.mServiceReceiver);
                this.mRemoveCallback = callback;
                return true;
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remove failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "remove failed because get service failed");
            return false;
        }
    }

    public boolean removeAll(RemoveCallback callback) {
        if (callback == null) {
            return false;
        }
        this.mService = getService();
        if (this.mService != null) {
            try {
                this.mService.removeAll(this.mToken, 0, this.mServiceReceiver);
                this.mRemoveCallback = callback;
                return true;
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("remove failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "remove failed because get service failed");
            return false;
        }
    }

    public int[] getEnrolledVoiceIdList() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                return this.mService.getEnrolledVoiceIdList(0, "");
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getEnrolledVoiceIdList failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "getEnrolledVoiceIdList failed because get service failed");
            return null;
        }
    }

    public long preEnroll() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                return this.mService.preEnroll();
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("preEnroll failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "preEnroll failed because get service failed");
            return 0;
        }
    }

    public boolean postEnroll() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                if (this.mService.postEnroll() == 0) {
                    return true;
                }
                return false;
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("postEnroll failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "postEnroll failed because get service failed");
            return false;
        }
    }

    public void resetTimeout() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                this.mService.resetTimeout();
                return;
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("resetTimeout failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
                return;
            }
        }
        Slog.e(TAG, "resetTimeout failed because get service failed");
    }

    public int getRemainingNum() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                return this.mService.getRemainingNum();
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getRemainingNum failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "getRemainingNum failed because get service failed");
            return -1;
        }
    }

    public int getTotalAuthFailedTimes() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                return this.mService.getTotalAuthFailedTimes();
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getTotalAuthFailedTimes failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "getTotalAuthFailedTimes failed because get service failed");
            return -1;
        }
    }

    public long getRemainingTime() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                return this.mService.getRemainingTime();
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getRemainingTime failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "getRemainingTime failed because get service failed");
            return -1;
        }
    }

    public int getHeadsetStatus() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                return this.mService.getHeadsetStatus();
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getHeadsetStatus failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "getHeadsetStatus failed because get service failed");
            return -1;
        }
    }

    public List<String> getVoiceEnrollStringList() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                return this.mService.getVoiceEnrollStringList();
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getVoiceEnrollStringList failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "getVoiceEnrollStringList failed because get service failed");
            return null;
        }
    }

    public int getHeadsetStatus(String mac) {
        this.mService = getService();
        if (this.mService != null) {
            try {
                return this.mService.getHeadsetStatusByMac(mac);
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getHeadsetStatusByMac failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "getHeadsetStatusByMac failed because get service failed");
            return -1;
        }
    }

    public VoiceCommandList getVoiceCommandList() {
        this.mService = getService();
        if (this.mService != null) {
            try {
                return this.mService.getVoiceCommandList();
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("getVoiceCommandList failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        } else {
            Slog.e(TAG, "getVoiceCommandList failed because get service failed");
            return null;
        }
    }

    private void sendEnrollResult(int ret, int id) {
        if (this.mEnrollCallback != null) {
            this.mEnrollCallback.onEnrollResult(ret, id);
        } else {
            Slog.e(TAG, "enroll result send failed because callback is null");
        }
    }

    private void sendEnrollAcquire(int aquireInfo, int subInfo) {
        if (this.mEnrollCallback != null) {
            this.mEnrollCallback.onEnrollAcquire(aquireInfo, subInfo);
        } else {
            Slog.e(TAG, "acquire info send failed because callback is null");
        }
    }

    private void sendEnrollCancelResult(int ret) {
        if (this.mEnrollCallback != null) {
            this.mEnrollCallback.onEnrollCancelResult(ret);
        } else {
            Slog.e(TAG, "enroll cancel result send failed because callback is null");
        }
    }

    private void sendRemoveResult(int ret) {
        if (this.mRemoveCallback != null) {
            this.mRemoveCallback.onRemoveResult(ret);
        } else {
            Slog.e(TAG, "remove result send failed because callback is null");
        }
    }

    private void sendAuthSuccess(int userId, int type) {
        if (this.mAuthCallback != null) {
            boolean ret = this.mAuthCallback.onReceiveAuthVoice(userId, type);
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Send auth result returns ");
            stringBuilder.append(ret);
            Slog.i(str, stringBuilder.toString());
            if (ret) {
                startVoiceActivity(type);
                return;
            }
            return;
        }
        Slog.e(TAG, "auth ok send failed because callback is null");
    }

    private void startVoiceActivity(int type) {
        this.mService = getService();
        if (this.mService != null) {
            try {
                this.mService.startVoiceActivity(type);
                return;
            } catch (RemoteException e) {
                this.mService = null;
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("startVoiceActivity failed because ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
                return;
            }
        }
        Slog.e(TAG, "startVoiceActivity failed because get service failed");
    }

    private void sendAuthFailed(int userId, int type) {
        if (this.mAuthCallback != null) {
            this.mAuthCallback.onReceiveUnAuthVoice(userId, type, 0);
        } else {
            Slog.e(TAG, "auth fail send failed because callback is null");
        }
    }

    private void sendHeadsetStatusChange(int status) {
        if (this.mAuthCallback != null) {
            this.mAuthCallback.onHeadsetStatusChange(status);
        } else {
            Slog.e(TAG, "authCallback send HeadsetStatusChange failed because callback is null");
        }
    }

    private void sendHeadsetStatusChangedToClient(int status) {
        try {
            if (this.mHeadsetStatusCb != null) {
                this.mHeadsetStatusCb.onHeadsetStatusChange(status);
                return;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("sendHeadsetStatusChanged to client ");
            stringBuilder.append(status);
            Slog.e(str, stringBuilder.toString());
        } catch (Exception e) {
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("sendHeadsetStatusChangedToClient fail ");
            stringBuilder2.append(e);
            Slog.e(str2, stringBuilder2.toString());
        }
    }

    private synchronized IVoiceRecognizeService getService() {
        if (this.mService == null) {
            if (this.mServiceBinder != null) {
                this.mServiceBinder.unlinkToDeath(this, 0);
            }
            this.mServiceBinder = ServiceManager.getService(VOICERECOGNIZE_SERVICE);
            if (this.mServiceBinder == null) {
                Slog.e(TAG, "getService binder null");
                return null;
            }
            this.mService = IVoiceRecognizeService.Stub.asInterface(this.mServiceBinder);
            if (this.mService == null) {
                Slog.e(TAG, "getService Service null");
            } else {
                try {
                    this.mServiceBinder.linkToDeath(this, 0);
                } catch (RemoteException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("getService linkToDeath fail");
                    stringBuilder.append(e.getMessage());
                    Slog.e(str, stringBuilder.toString());
                }
            }
        }
        return this.mService;
    }

    public void binderDied() {
        synchronized (this) {
            Slog.e(TAG, "service binder died");
            this.mServiceBinder.unlinkToDeath(this, 0);
            this.mServiceBinder = null;
            this.mService = null;
        }
    }
}
