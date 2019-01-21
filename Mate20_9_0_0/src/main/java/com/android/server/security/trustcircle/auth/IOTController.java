package com.android.server.security.trustcircle.auth;

import android.content.Context;
import android.os.RemoteException;
import com.android.server.security.trustcircle.auth.AuthPara.InitAuthInfo;
import com.android.server.security.trustcircle.auth.AuthPara.RecAckInfo;
import com.android.server.security.trustcircle.auth.AuthPara.RecAuthAckInfo;
import com.android.server.security.trustcircle.auth.AuthPara.RecAuthInfo;
import com.android.server.security.trustcircle.auth.AuthPara.ReqPkInfo;
import com.android.server.security.trustcircle.auth.AuthPara.RespPkInfo;
import com.android.server.security.trustcircle.task.HwSecurityEventTask;
import com.android.server.security.trustcircle.task.HwSecurityTaskBase;
import com.android.server.security.trustcircle.task.HwSecurityTaskBase.RetCallback;
import com.android.server.security.trustcircle.task.HwSecurityTaskThread;
import com.android.server.security.trustcircle.utils.AuthUtils;
import com.android.server.security.trustcircle.utils.LogHelper;
import huawei.android.security.IAuthCallback;
import huawei.android.security.IKaCallback;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IOTController {
    public static final int AUTH_ID_ERROR = -1;
    public static final int EV_CANCEL_AUTH = 100;
    public static final int EV_CANCEL_AUTH_ALL = 104;
    public static final int EV_IOT_AUTH_ACK = 101;
    public static final int EV_IOT_REC_ACK = 103;
    public static final int EV_IOT_REC_PK = 102;
    public static final int ID_CANCEL_ALL = -2;
    public static final int RESULT_OK = 0;
    public static final int RESULT_REQ_PK = 1;
    private static final String TAG = IOTController.class.getSimpleName();
    public static final int TYPE_MASTER = 1000;
    public static final int TYPE_SLAVE = 1001;
    private static Object mLock = new Object();
    private static long sGenAuthID;
    private static volatile IOTController sInstance;
    Map<Long, InitAuthInfo> mClientsMaster = new ConcurrentHashMap();
    Map<Long, RecAuthInfo> mClientsSlave = new ConcurrentHashMap();
    RetCallback mInitCallback = new RetCallback() {
        public void onTaskCallback(HwSecurityTaskBase base, int ret) {
            String access$000 = IOTController.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("auth onTaskCallback, ret: ");
            stringBuilder.append(ret);
            LogHelper.d(access$000, stringBuilder.toString());
            long authID = ((IOTAuthTask) base).getAuthID();
            if (ret == 2 || ret == 1) {
                AuthUtils.processCancelAuth(authID);
                IOTController.this.notifyAuthExited(authID, ret, 1000);
            }
            IOTController.this.mClientsMaster.remove(Long.valueOf(authID));
        }
    };
    RetCallback mRecCallback = new RetCallback() {
        public void onTaskCallback(HwSecurityTaskBase base, int ret) {
            String access$000 = IOTController.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("receive auth onTaskCallback, ret: ");
            stringBuilder.append(ret);
            LogHelper.d(access$000, stringBuilder.toString());
            long authID = ((RecAuthTask) base).getAuthID();
            if (ret == 2 || ret == 1) {
                AuthUtils.processCancelAuth(authID);
                IOTController.this.notifyAuthExited(authID, ret, 1001);
            }
            IOTController.this.mClientsSlave.remove(Long.valueOf(authID));
        }
    };

    public static class KaInfoRequest {
        public byte[] aesTmpKey;
        public long authId;
        public String kaInfo;
        public int kaVersion;
        public long userId;

        public KaInfoRequest(long authId, int kaVersion, long userId, byte[] aesTmpKey, String kaInfo) {
            this.authId = authId;
            this.kaVersion = kaVersion;
            this.userId = userId;
            this.aesTmpKey = (byte[]) aesTmpKey.clone();
            this.kaInfo = kaInfo;
        }
    }

    public static class KaInfoResponse {
        public byte[] iv;
        public byte[] payload;
        public int result;

        public KaInfoResponse(int result, byte[] iv, byte[] payload) {
            this.result = result;
            this.iv = (byte[]) iv.clone();
            this.payload = (byte[]) payload.clone();
        }
    }

    private IOTController() {
    }

    public static IOTController getInstance() {
        if (sInstance == null) {
            synchronized (IOTController.class) {
                if (sInstance == null) {
                    sInstance = new IOTController();
                }
            }
        }
        return sInstance;
    }

    public long initKeyAgreement(IKaCallback callback, int kaVersion, long userId, byte[] aesTmpKey, String kaInfo, Context context) {
        long authId = generateAuthID();
        HwSecurityTaskThread.staticPushTask(new KaRequestTask(null, null, context, new KaInfoRequest(authId, kaVersion, userId, aesTmpKey, kaInfo), callback), 1);
        return authId;
    }

    public long initAuth(IAuthCallback callback, int authType, int authVersion, int policy, long userID, byte[] AESTmpKey) {
        long authID = generateAuthID();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("start auth, authId: ");
        stringBuilder.append(authID);
        LogHelper.d(str, stringBuilder.toString());
        InitAuthInfo info = new InitAuthInfo(callback, authID, authType, authVersion, policy, userID, AESTmpKey);
        this.mClientsMaster.put(Long.valueOf(authID), info);
        HwSecurityTaskThread.staticPushTask(new IOTAuthTask(null, this.mInitCallback, info), 1);
        return authID;
    }

    public long receiveAuthSync(IAuthCallback callback, int authType, int authVersion, int taVersion, int policy, long userID, byte[] AESTmpKey, byte[] tcisId, int pkVersion, long nonce, int authKeyAlgoType, byte[] authKeyInfo, byte[] authKeyInfoSign) {
        long authID = generateAuthID();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("receive auth, authID: ");
        stringBuilder.append(authID);
        LogHelper.d(str, stringBuilder.toString());
        long authID2 = authID;
        RecAuthInfo info = new RecAuthInfo(authID, callback, authType, authVersion, (short) taVersion, policy, userID, AESTmpKey, tcisId, pkVersion, nonce, (short) authKeyAlgoType, authKeyInfo, authKeyInfoSign);
        long authID3 = authID2;
        this.mClientsSlave.put(Long.valueOf(authID3), info);
        HwSecurityTaskThread.staticPushTask(new RecAuthTask(null, this.mRecCallback, info), 1);
        return authID3;
    }

    public boolean receiveAuthSyncAck(long authID, byte[] tcisIdSlave, int pkVersionSlave, long nonceSlave, byte[] mac, int authKeyAlgoTypeSlave, byte[] authKeyInfoSlave, byte[] authKeyInfoSignSlave) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("receive authAck, authID: ");
        long j = authID;
        stringBuilder.append(j);
        LogHelper.d(str, stringBuilder.toString());
        HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new AuthSyncAckEv(101, new RecAuthAckInfo(j, tcisIdSlave, pkVersionSlave, nonceSlave, mac, (short) authKeyAlgoTypeSlave, authKeyInfoSlave, authKeyInfoSignSlave))), 1);
        return true;
    }

    public boolean requestPK(long authID, long userID) {
        int type;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("IOT requestPK, authID: ");
        stringBuilder.append(authID);
        LogHelper.d(str, stringBuilder.toString());
        ReqPkInfo reqPkInfo = new ReqPkInfo(userID, authID);
        if (this.mClientsMaster.containsKey(Long.valueOf(authID))) {
            type = 1000;
        } else if (!this.mClientsSlave.containsKey(Long.valueOf(authID))) {
            return false;
        } else {
            type = 1001;
        }
        HwSecurityTaskThread.staticPushTask(new IOTReqPkTask(null, null, reqPkInfo, type), 1);
        return true;
    }

    public boolean receivePK(long authID, int authKeyAlgoType, byte[] authKeyData, byte[] authKeyDataSign) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("receivePK, authID: ");
        stringBuilder.append(authID);
        LogHelper.d(str, stringBuilder.toString());
        HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new ReceivePkEv(102, new RespPkInfo(authID, (short) authKeyAlgoType, authKeyData, authKeyDataSign, 0))), 1);
        return true;
    }

    public boolean receiveAck(long authID, byte[] mac) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("receiveAck, authID: ");
        stringBuilder.append(authID);
        LogHelper.d(str, stringBuilder.toString());
        HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new ReceiveAckEv(103, new RecAckInfo(authID, mac))), 1);
        return true;
    }

    public boolean cancelAuth(long authID) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("cancelAuth, authID: ");
        stringBuilder.append(authID);
        LogHelper.i(str, stringBuilder.toString());
        HwSecurityTaskThread.staticPushTask(new HwSecurityEventTask(new CancelAuthEv(100, authID)), 0);
        return true;
    }

    private long generateAuthID() {
        long j;
        synchronized (mLock) {
            j = sGenAuthID + 1;
            sGenAuthID = j;
        }
        return j;
    }

    public IAuthCallback getIOTCallback(long authID, int type) {
        if (type == 1000 && this.mClientsMaster != null) {
            InitAuthInfo info = (InitAuthInfo) this.mClientsMaster.get(Long.valueOf(authID));
            if (info != null) {
                return info.mCallback;
            }
            return null;
        } else if (type != 1001 || this.mClientsSlave == null) {
            return null;
        } else {
            RecAuthInfo info2 = (RecAuthInfo) this.mClientsSlave.get(Long.valueOf(authID));
            if (info2 != null) {
                return info2.mCallback;
            }
            return null;
        }
    }

    private void notifyAuthExited(long authID, int ret, int type) {
        String str;
        StringBuilder stringBuilder;
        IAuthCallback callback = getIOTCallback(authID, type);
        if (callback == null) {
            return;
        }
        if (ret == 2) {
            try {
                callback.onAuthExited(authID, 2046820389);
            } catch (RemoteException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("notifyAuthExited failed, ");
                stringBuilder.append(e.toString());
                LogHelper.e(str, stringBuilder.toString());
            } catch (Exception e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("notifyAuthExited failed, ");
                stringBuilder.append(e2.toString());
                LogHelper.e(str, stringBuilder.toString());
            }
        } else if (ret == 1) {
            callback.onAuthExited(authID, 2046820388);
        }
    }
}
