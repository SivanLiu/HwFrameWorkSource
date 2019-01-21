package com.huawei.hsm.permission;

import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.hsm.HwSystemManager.Notifier;
import android.os.Binder;
import android.os.Bundle;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.hsm.permission.StubController.HoldServiceDieListener;
import com.huawei.hsm.permission.monitor.PermRecordHandler;
import com.huawei.permission.IHoldNotifier.Stub;
import java.util.ArrayList;
import java.util.List;

public class SmsPermission {
    public static final int AUTHENTICATE_RESULT_ALLOW = 0;
    public static final int AUTHENTICATE_RESULT_ALLOW_FOREVER = 1;
    public static final int AUTHENTICATE_RESULT_DISALLOW = 2;
    public static final int AUTHENTICATE_RESULT_DISALLOW_FOREVER = 3;
    private static final String DIVIDER_CHAR = ":";
    public static final String KEY_AUTHENTICATE_RESULT = "sms_authenticate_result_list";
    public static final String KEY_SMS_ID = "sms_id_list";
    public static final int NEED_AUTHENTICATE = 4;
    public static final int PARAMETER_INVALID = 6;
    private static final int RESULT_ERROR_GENERIC_FAILURE = 1;
    public static final int SERVICE_EXCEPTION = 5;
    private static final String TAG = "SmsPermission";
    private static boolean isControl = SystemProperties.getBoolean("ro.config.hw_wirenetcontrol", false);
    private static SmsPermission mSmsPermission;
    private Context mContext = null;
    private int mLastSmsId;
    private final NotifierBinder mNotifierBinder = new NotifierBinder();
    private final ServiceDieListener mServiceDieListener = new ServiceDieListener();
    private Notifier mSmsNotifier;

    private final class ServiceDieListener implements HoldServiceDieListener {
        private ServiceDieListener() {
        }

        public void notifyServiceDie() {
            String str = SmsPermission.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("notifyServiceDie The HoldService is DIED! lastSms Id = ");
            stringBuilder.append(SmsPermission.this.mLastSmsId);
            Log.e(str, stringBuilder.toString());
            Bundle bundle = SmsPermission.packDataOfException(SmsPermission.this.mLastSmsId, 5);
            if (SmsPermission.this.mSmsNotifier != null) {
                SmsPermission.this.mSmsNotifier.notifyResult(bundle);
            }
        }
    }

    private final class NotifierBinder extends Stub {
        private NotifierBinder() {
        }

        public int notifyResult(String owner, Bundle bundle) {
            if (SmsPermission.this.mSmsNotifier != null) {
                Log.d(SmsPermission.TAG, "notifyAuthResultOfSmsSend authenticate complete!");
                SmsPermission.this.mSmsNotifier.notifyResult(bundle);
            } else {
                Log.e(SmsPermission.TAG, "notifyAuthResultOfSmsSend Notifier is NULL!");
            }
            return 0;
        }
    }

    private SmsPermission() {
        StubController.addServiceDieListener(this.mServiceDieListener);
    }

    public static SmsPermission getInstance() {
        SmsPermission smsPermission;
        synchronized (SmsPermission.class) {
            if (mSmsPermission == null) {
                mSmsPermission = new SmsPermission();
            }
            smsPermission = mSmsPermission;
        }
        return smsPermission;
    }

    private void recordPermissionUsed(int uid, int pid) {
        PermRecordHandler mPermRecHandler = PermRecordHandler.getHandleInstance();
        if (mPermRecHandler != null) {
            mPermRecHandler.accessPermission(uid, pid, 32, null);
        }
    }

    public boolean isMmsBlocked() {
        if (!isControl) {
            return false;
        }
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        if (StubController.checkPreBlock(uid, 32)) {
            return true;
        }
        if (!StubController.checkPrecondition(uid)) {
            recordPermissionUsed(uid, pid);
            return false;
        } else if (!StubController.isGlobalSwitchOn(this.mContext, 32)) {
            return false;
        } else {
            int selectionResult = StubController.holdForGetPermissionSelection(32, uid, pid, null);
            if (selectionResult == 0) {
                Log.e(TAG, "Get selection error");
                return false;
            } else if (2 == selectionResult) {
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean isSmsBlocked(String destAddr, String smsBody, PendingIntent sentIntent) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        if (StubController.checkPreBlock(uid, 32)) {
            return true;
        }
        if (StubController.checkPrecondition(uid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(destAddr);
            stringBuilder.append(DIVIDER_CHAR);
            stringBuilder.append(smsBody);
            int selectionResult = StubController.holdForGetPermissionSelection(32, uid, pid, stringBuilder.toString());
            if (selectionResult == 0) {
                Log.e(TAG, "Get selection error");
                return false;
            } else if (2 != selectionResult) {
                return false;
            } else {
                sendFakeIntent(sentIntent);
                return true;
            }
        }
        recordPermissionUsed(uid, pid);
        return false;
    }

    public boolean isSmsBlocked(String destAddr, String smsBody, List<PendingIntent> sentIntents) {
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        if (StubController.checkPreBlock(uid, 32)) {
            return true;
        }
        if (StubController.checkPrecondition(uid)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(destAddr);
            stringBuilder.append(DIVIDER_CHAR);
            stringBuilder.append(smsBody);
            int selectionResult = StubController.holdForGetPermissionSelection(32, uid, pid, stringBuilder.toString());
            if (selectionResult == 0) {
                Log.e(TAG, "Get selection error");
                return false;
            } else if (2 != selectionResult) {
                return false;
            } else {
                sendFakeIntents(sentIntents);
                return true;
            }
        }
        recordPermissionUsed(uid, pid);
        return false;
    }

    public void authenticateSmsSend(Notifier callback, int uidOf3RdApk, int smsId, String smsBody, String smsAddress) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("authenticateSmsSend  uid = ");
        stringBuilder.append(uidOf3RdApk);
        stringBuilder.append(" smsid = ");
        stringBuilder.append(smsId);
        Log.d(str, stringBuilder.toString());
        if (isParamInvalidForAuth(callback, uidOf3RdApk, smsId, smsBody, smsAddress)) {
            Log.d(TAG, "authenticateSmsSend PARAM IS INVALID!");
            if (callback != null) {
                callback.notifyResult(packDataOfException(smsId, 6));
            }
            return;
        }
        this.mSmsNotifier = callback;
        this.mLastSmsId = smsId;
        if (StubController.authenticateSmsSend(this.mNotifierBinder, uidOf3RdApk, smsId, smsBody, smsAddress) != 0) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("authenticateSmsSend call service exception! sms id = ");
            stringBuilder.append(smsId);
            Log.e(str, stringBuilder.toString());
            callback.notifyResult(packDataOfException(smsId, 5));
        }
    }

    private static Bundle packDataOfException(int smsId, int result) {
        Bundle bundle = new Bundle();
        ArrayList<Integer> smsIdList = new ArrayList();
        smsIdList.add(Integer.valueOf(smsId));
        bundle.putIntegerArrayList(KEY_SMS_ID, smsIdList);
        ArrayList<Integer> resultList = new ArrayList();
        resultList.add(Integer.valueOf(result));
        bundle.putIntegerArrayList(KEY_AUTHENTICATE_RESULT, resultList);
        return bundle;
    }

    private static boolean isParamInvalidForAuth(Notifier notifyResult, int callingUid, int smsId, String smsBody, String smsAddress) {
        return notifyResult == null || callingUid < 0 || Binder.getCallingUid() != callingUid || TextUtils.isEmpty(smsAddress);
    }

    private static void sendFakeIntents(List<PendingIntent> sentIntents) {
        if (sentIntents != null && !sentIntents.isEmpty()) {
            for (int i = 0; i < sentIntents.size(); i++) {
                sendFakeIntent((PendingIntent) sentIntents.get(i));
            }
        }
    }

    private static void sendFakeIntent(PendingIntent PI) {
        if (PI != null) {
            try {
                PI.send(1);
            } catch (CanceledException e) {
                e.printStackTrace();
            }
        }
    }
}
