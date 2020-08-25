package com.huawei.hiai.awareness.service;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;
import android.util.Log;
import com.huawei.hiai.awareness.AwarenessConstants;
import com.huawei.hiai.awareness.AwarenessInnerConstants;
import com.huawei.hiai.awareness.awareness.Event;
import com.huawei.hiai.awareness.common.Utils;
import com.huawei.hiai.awareness.common.log.LogUtil;
import com.huawei.hiai.awareness.movement.MovementController;
import com.huawei.hiai.awareness.service.IAwarenessService;

public class AwarenessManager {
    private static final int MSDP_SERVICE_CONNECTION_CYCLE = 2000;
    private static final int MSG_MSDP_SERVICE_CONNECTION = 1;
    private static final String TAG = "AwarenessManager";
    private static final String THREAD_MONITOR_MSDP_CONNECTION = "MonitorMSDPConnectionThread";
    private static final int TRY_CONNECT_MSDP_TIMES = 10;
    private static boolean mIsAwarenessInstalled = false;
    /* access modifiers changed from: private */
    public AwarenessServiceConnection mAwarenessServiceConnection = null;
    private Context mContext = null;
    private MsdpConMsgHandler mHandler;
    /* access modifiers changed from: private */
    public IAwarenessService mIAwarenessService;
    /* access modifiers changed from: private */
    public boolean mIsConnectAwarenessService = false;
    private ServiceConnection mServiceConnection = new ServiceConnection() {
        /* class com.huawei.hiai.awareness.service.AwarenessManager.AnonymousClass1 */

        public void onServiceConnected(ComponentName name, IBinder service) {
            IAwarenessService unused = AwarenessManager.this.mIAwarenessService = IAwarenessService.Stub.asInterface(service);
            if (AwarenessManager.this.mIAwarenessService == null) {
                LogUtil.e(AwarenessManager.TAG, "mIAwarenessService is null");
                if (AwarenessManager.this.mAwarenessServiceConnection != null) {
                    LogUtil.i(AwarenessManager.TAG, "AwarenessServiceConnection is not null, service disconnect");
                    AwarenessManager.this.mAwarenessServiceConnection.onServiceDisconnected();
                }
            } else {
                boolean unused2 = AwarenessManager.this.mIsConnectAwarenessService = true;
                if (AwarenessManager.this.mAwarenessServiceConnection != null) {
                    LogUtil.i(AwarenessManager.TAG, "service connect");
                    AwarenessManager.this.mAwarenessServiceConnection.onServiceConnected();
                }
            }
            LogUtil.d(AwarenessManager.TAG, "onServiceConnected mIAwarenessService " + AwarenessManager.this.mIAwarenessService);
        }

        public void onServiceDisconnected(ComponentName name) {
            boolean unused = AwarenessManager.this.mIsConnectAwarenessService = false;
            if (AwarenessManager.this.mAwarenessServiceConnection != null) {
                LogUtil.i(AwarenessManager.TAG, "service disconnect");
                AwarenessManager.this.mAwarenessServiceConnection.onServiceDisconnected();
            }
        }
    };
    private int mTryConnectionTimes = 0;

    public AwarenessManager(Context context) {
        LogUtil.d(TAG, "AwarenessManager()");
        if (context != null) {
            this.mContext = context;
        } else {
            LogUtil.e(TAG, "AwarenessManager() context == null");
        }
    }

    public boolean connectService(AwarenessServiceConnection awarenessServiceConnection) {
        LogUtil.d(TAG, "connectService()");
        if (this.mContext == null) {
            LogUtil.e(TAG, "connectService() mContext == null");
            return false;
        }
        if (awarenessServiceConnection != null) {
            this.mAwarenessServiceConnection = awarenessServiceConnection;
            if (!this.mIsConnectAwarenessService) {
                this.mIsConnectAwarenessService = bindService();
            } else {
                LogUtil.i(TAG, "connectService() awarenessService is connected");
            }
        } else {
            LogUtil.e(TAG, "connectService() awarenessServiceConnection == null");
        }
        LogUtil.i(TAG, "connectService() mIsConnectAwarenessService = " + this.mIsConnectAwarenessService + ",getPackageName: " + this.mContext.getPackageName());
        return this.mIsConnectAwarenessService;
    }

    /* access modifiers changed from: private */
    public void dealWithMsdpConnction() {
        if (this.mContext == null) {
            LogUtil.e(TAG, "dealWithMsdpConnction() mContext == null");
            return;
        }
        LogUtil.i(TAG, "dealWithMsdpConnction() packageName: " + this.mContext.getPackageName());
        if (isConnectMsdpMovementServer()) {
            LogUtil.d(TAG, "dealWithMsdpConnction(), quit handler!");
            MsdpConMsgHandler msdpConMsgHandler = this.mHandler;
            if (msdpConMsgHandler != null) {
                msdpConMsgHandler.getLooper().quit();
                this.mHandler = null;
            }
            this.mTryConnectionTimes = 0;
            return;
        }
        LogUtil.d(TAG, "dealWithMsdpConnction() mTryConnectionTimes:" + this.mTryConnectionTimes);
        if (this.mHandler != null && this.mTryConnectionTimes < 10) {
            ConnectServiceManager.getInstance().onStart();
            this.mTryConnectionTimes++;
            LogUtil.d(TAG, "dealWithMsdpConnction() send message after 2s");
            this.mHandler.sendEmptyMessageDelayed(1, 2000);
        }
    }

    private final class MsdpConMsgHandler extends Handler {
        MsdpConMsgHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what != 1) {
                Log.e(AwarenessManager.TAG, "handleMessage error msg.");
                return;
            }
            LogUtil.d(AwarenessManager.TAG, "receive msdp connection request!");
            AwarenessManager.this.dealWithMsdpConnction();
        }
    }

    public boolean connectMsdpService(AwarenessServiceConnection awarenessServiceConnection) {
        LogUtil.d(TAG, "connectMsdpService()");
        Context context = this.mContext;
        if (context == null) {
            LogUtil.e(TAG, "connectMsdpService() mContext == null");
            return false;
        } else if (!Utils.isMsdpInstalled(context.getApplicationContext())) {
            LogUtil.e(TAG, "connectMsdpService() msdp do not install");
            return false;
        } else {
            HandlerThread handleThread = new HandlerThread(THREAD_MONITOR_MSDP_CONNECTION);
            handleThread.start();
            this.mHandler = new MsdpConMsgHandler(handleThread.getLooper());
            ConnectServiceManager.getInstance().initialize(this.mContext);
            ConnectServiceManager.getInstance().setConnectServiceManagerContext(this.mContext);
            this.mAwarenessServiceConnection = awarenessServiceConnection;
            ConnectServiceManager.getInstance().setAwarenessServiceConnection(this.mAwarenessServiceConnection);
            this.mTryConnectionTimes = 0;
            this.mHandler.sendEmptyMessage(1);
            return true;
        }
    }

    private boolean bindService() {
        if (this.mContext == null) {
            LogUtil.e(TAG, "bindService() mContext == null");
            return false;
        }
        LogUtil.d(TAG, "bindService()");
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("com.huawei.hiai", AwarenessConstants.AWARENESS_SERVICE_CLASS_NAME));
        intent.setAction(AwarenessConstants.AWARENESS_SERVICE_ACTION_NAME);
        intent.putExtra(AwarenessConstants.LAUNCH_AWARENESS_PACKAGE_NAME, this.mContext.getPackageName());
        try {
            return this.mContext.bindService(intent, this.mServiceConnection, 1);
        } catch (SecurityException e) {
            LogUtil.e(TAG, "bindService() SecurityException");
            return false;
        }
    }

    public boolean disconnectService() {
        LogUtil.d(TAG, "disconnectService()");
        if (isConnectMsdpMovementServer()) {
            ConnectServiceManager.getInstance().stopConnectService();
            AwarenessServiceConnection awarenessServiceConnection = this.mAwarenessServiceConnection;
            if (awarenessServiceConnection != null) {
                awarenessServiceConnection.onServiceDisconnected();
            }
            return true;
        } else if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "disconnectService() mIAwarenessService == null.");
            return false;
        } else {
            if (this.mContext != null) {
                LogUtil.d(TAG, "disconnectService() unbindService");
                this.mContext.unbindService(this.mServiceConnection);
            }
            AwarenessServiceConnection awarenessServiceConnection2 = this.mAwarenessServiceConnection;
            if (awarenessServiceConnection2 != null) {
                awarenessServiceConnection2.onServiceDisconnected();
            }
            this.mIsConnectAwarenessService = false;
            return true;
        }
    }

    public RequestResult getCurrentMotionStatus() {
        LogUtil.d(TAG, "getCurrentMotionStatus()");
        RequestResult result = getCurrentStatus(1);
        LogUtil.d(TAG, "getCurrentMotionStatus() result : " + result);
        return result;
    }

    public RequestResult getCurrentPhoneStatus() {
        LogUtil.d(TAG, "getCurrentPhoneStatus()");
        RequestResult result = getCurrentStatus(3);
        LogUtil.d(TAG, "getCurrentPhoneStatus() result : " + result);
        return result;
    }

    public String getAwarenessApiVersion() {
        LogUtil.d(TAG, "getAwarenessApiVersion()");
        if (isConnectMsdpMovementServer()) {
            if (isMsdpIntegrateSensorHub()) {
                return AwarenessInnerConstants.AWARENESS_VERSION_CODE;
            }
            LogUtil.e(TAG, "getAwarenessApiVersion() old version!");
            return null;
        } else if (isConnectAwarenessService()) {
            try {
                LogUtil.d(TAG, "getAwarenessApiVersion() call binder");
                String version = this.mIAwarenessService.getAwarenessApiVersion();
                LogUtil.d(TAG, "getAwarenessApiVersion() version : " + version);
                return version;
            } catch (RemoteException e) {
                LogUtil.e(TAG, "getAwarenessApiVersion() RemoteException");
                return null;
            }
        } else {
            LogUtil.e(TAG, "getAwarenessApiVersion() awarenessService is not connect");
            return null;
        }
    }

    private boolean isMsdpIntegrateSensorHub() {
        return ConnectServiceManager.getInstance().isIntegrateSensorHub();
    }

    public RequestResult getCurrentAwareness(int type) {
        return getCurrentAwareness(type, false, null);
    }

    public RequestResult getCurrentAwareness(int type, boolean isCustom, Bundle bundle) {
        LogUtil.d(TAG, "getCurrentAwareness() type : " + type + " isCustom : " + isCustom);
        if (isConnectMsdpMovementServer() && type == 1) {
            return getCurrentStatus(type);
        }
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "getCurrentAwareness() mIAwarenessService == null");
            RequestResult result = new RequestResult(AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED_CODE, AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED);
            result.setResultType(3);
            return result;
        } else if (this.mContext == null) {
            LogUtil.e(TAG, "getCurrentAwareness() mContext == null");
            RequestResult result2 = new RequestResult(AwarenessConstants.ERROR_PARAMETER_CODE, AwarenessConstants.ERROR_PARAMETER);
            result2.setResultType(3);
            return result2;
        } else if (isConnectAwarenessService()) {
            try {
                LogUtil.d(TAG, "getCurrentAwareness() call binder");
                return this.mIAwarenessService.getCurrentAwareness(type, isCustom, bundle, this.mContext.getPackageName());
            } catch (RemoteException e) {
                LogUtil.e(TAG, "getCurrentAwareness() RemoteException");
                RequestResult result3 = new RequestResult(AwarenessConstants.ERROR_REMOTE_CALLBACK_CODE, AwarenessConstants.ERROR_REMOTE_CALLBACK);
                result3.setResultType(3);
                return result3;
            }
        } else {
            LogUtil.e(TAG, "getCurrentAwareness() awarenessService is not connect");
            RequestResult result4 = new RequestResult(AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED_CODE, AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED);
            result4.setResultType(4);
            return result4;
        }
    }

    public boolean registerMotionFence(IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerMotionFence() awarenessFence : " + awarenessFence);
        boolean isRegisterSuccess = registerFence(1, -1, callback, awarenessFence, pendingOperation);
        LogUtil.d(TAG, "registerMotionFence()  isRegisterSuccess : " + isRegisterSuccess);
        return isRegisterSuccess;
    }

    public boolean registerTimeFence(IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerTimeFence() awarenessFence : " + awarenessFence);
        boolean isRegisterSuccess = registerFence(8, -1, callback, awarenessFence, pendingOperation);
        LogUtil.d(TAG, "registerTimeFence()  isRegisterSuccess : " + isRegisterSuccess);
        return isRegisterSuccess;
    }

    public boolean registerLocationFence(IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerLocationFence() awarenessFence : " + awarenessFence);
        boolean isRegisterSuccess = registerFence(6, -1, callback, awarenessFence, pendingOperation);
        LogUtil.d(TAG, "registerLocationFence()  isRegisterSuccess : " + isRegisterSuccess);
        return isRegisterSuccess;
    }

    public boolean registerCustomLocationFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation) {
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "registerCustomLocationFence() mIAwarenessService = null");
            return false;
        } else if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerCustomLocationFence() param error");
            return false;
        } else if (isConnectAwarenessService()) {
            try {
                awarenessFence.build(this.mContext);
                LogUtil.d(TAG, "registerCustomLocationFence() call binder awarenessFence :" + awarenessFence);
                if (AwarenessConstants.LOCATION_CUSTOM.equals(awarenessFence.getSecondAction())) {
                    boolean isRegisterSuccess = this.mIAwarenessService.registerCustomLocationFence(callback, awarenessFence, null, pendingOperation);
                    LogUtil.d(TAG, "registerCustomLocationFence()  isRegisterSuccess : " + isRegisterSuccess);
                    return isRegisterSuccess;
                }
                LogUtil.e(TAG, "registerCustomLocationFence() secondAction error");
                return false;
            } catch (RemoteException e) {
                LogUtil.e(TAG, "registerCustomLocationFence() RemoteException");
                return false;
            }
        } else {
            LogUtil.e(TAG, "registerCustomLocationFence() awarenessService is not connect");
            return false;
        }
    }

    public boolean registerAppUseTotalTimeFence(IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerAppUseTotalTimeFence() awarenessFence : " + awarenessFence);
        boolean isRegisterSuccess = registerFence(9, 1, callback, awarenessFence, pendingOperation);
        LogUtil.d(TAG, "registerAppUseTotalTimeFence()  isRegisterSuccess : " + isRegisterSuccess);
        return isRegisterSuccess;
    }

    public boolean registerOneAppContinuousUseTimeFence(IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerOneAppContinuousUseTimeFence() awarenessFence : " + awarenessFence);
        boolean isRegisterSuccess = registerFence(9, 2, callback, awarenessFence, pendingOperation);
        LogUtil.d(TAG, "registerOneAppContinuousUseTimeFence()  isRegisterSuccess : " + isRegisterSuccess);
        return isRegisterSuccess;
    }

    public boolean registerDeviceUseTotalTimeFence(IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerDeviceUseTotalTimeFence() awarenessFence : " + awarenessFence);
        boolean isRegisterSuccess = registerFence(9, 3, callback, awarenessFence, pendingOperation);
        LogUtil.d(TAG, "registerDeviceUseTotalTimeFence()  isRegisterSuccess : " + isRegisterSuccess);
        return isRegisterSuccess;
    }

    public boolean registerScreenUnlockTotalNumberFence(IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerScreenUnlockTotalNumberFence() awarenessFence : " + awarenessFence);
        boolean isRegisterSuccess = registerFence(9, 4, callback, awarenessFence, pendingOperation);
        LogUtil.d(TAG, "registerScreenUnlockTotalNumberFence()  isRegisterSuccess : " + isRegisterSuccess);
        return isRegisterSuccess;
    }

    public boolean registerScreenUnlockFence(IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "registerScreenUnlockFence() awarenessFence : " + awarenessFence);
        boolean isRegisterSuccess = registerFence(10, 1, callback, awarenessFence, pendingOperation);
        LogUtil.d(TAG, "registerScreenUnlockFence()  isRegisterSuccess : " + isRegisterSuccess);
        return isRegisterSuccess;
    }

    public boolean registerBroadcastEventFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "registerBroadcastEventFence() mIAwarenessService = null");
            return false;
        } else if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerBroadcastEventFence() param error");
            return false;
        } else {
            Bundle intentBundle = new Bundle();
            intentBundle.putParcelable(AwarenessConstants.REGISTER_BROADCAST_FENCE_INTENT, intent);
            awarenessFence.setRegisterBundle(intentBundle);
            if (isConnectAwarenessService()) {
                try {
                    awarenessFence.build(this.mContext);
                    LogUtil.d(TAG, "registerBroadcastEventFence() call binder awarenessFence :" + awarenessFence);
                    boolean isRegisterSuccess = this.mIAwarenessService.registerBroadcastEventFence(callback, awarenessFence, null, pendingOperation);
                    LogUtil.d(TAG, "registerBroadcastEventFence()  isRegisterSuccess : " + isRegisterSuccess);
                    return isRegisterSuccess;
                } catch (RemoteException e) {
                    LogUtil.e(TAG, "registerBroadcastEventFence() RemoteException");
                    return false;
                }
            } else {
                LogUtil.e(TAG, "registerBroadcastEventFence() awarenessService is not connect");
                return false;
            }
        }
    }

    public RequestResult getFenceTriggerResult(AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        RequestResult result;
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "getFenceTriggerResult() mIAwarenessService = null");
            RequestResult result2 = new RequestResult(AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED_CODE, AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED);
            result2.setResultType(9);
            return result2;
        } else if (awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "getFenceTriggerResult() param error");
            RequestResult result3 = new RequestResult(AwarenessConstants.ERROR_PARAMETER_CODE, AwarenessConstants.ERROR_PARAMETER);
            result3.setResultType(9);
            return result3;
        } else {
            LogUtil.d(TAG, "getFenceTriggerResult() awarenessFence : " + awarenessFence + " pendingOperation.hashCode : " + pendingOperation.hashCode());
            if (isConnectAwarenessService()) {
                try {
                    awarenessFence.build(this.mContext);
                    LogUtil.d(TAG, "getFenceTriggerResult() call binder");
                    if (awarenessFence instanceof ExtendAwarenessFence) {
                        LogUtil.d(TAG, "getFenceTriggerResult() revert to ExtendAwarenessFence");
                        result = this.mIAwarenessService.getExtendFenceTriggerResult((ExtendAwarenessFence) awarenessFence, null, pendingOperation);
                    } else {
                        LogUtil.d(TAG, "getFenceTriggerResult() is AwarenessFence");
                        result = this.mIAwarenessService.getFenceTriggerResult(awarenessFence, null, pendingOperation);
                    }
                    LogUtil.d(TAG, "getFenceTriggerResult() result : " + result);
                    return result;
                } catch (RemoteException e) {
                    LogUtil.e(TAG, "getFenceTriggerResult() RemoteException");
                    RequestResult result4 = new RequestResult(AwarenessConstants.ERROR_REMOTE_CALLBACK_CODE, AwarenessConstants.ERROR_REMOTE_CALLBACK);
                    result4.setResultType(9);
                    return result4;
                }
            } else {
                LogUtil.e(TAG, "getFenceTriggerResult() awarenessService is not connect");
                RequestResult result5 = new RequestResult(AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED_CODE, AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED);
                result5.setResultType(4);
                return result5;
            }
        }
    }

    public boolean unRegisterFence(IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        LogUtil.d(TAG, "unRegisterFence()");
        if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "unRegisterFence() param error");
            return false;
        } else if (isConnectMsdpMovementServer()) {
            awarenessFence.build(this.mContext);
            return AwarenessBinder.getInstance().unRegisterFence(callback, awarenessFence, null, pendingOperation);
        } else if (isConnectAwarenessService()) {
            LogUtil.d(TAG, "unRegisterFence() pendingOperation.hashCode : " + pendingOperation.hashCode());
            try {
                awarenessFence.build(this.mContext);
                LogUtil.d(TAG, "unRegisterFence() call binder awarenessFence :" + awarenessFence);
                if (awarenessFence instanceof ExtendAwarenessFence) {
                    LogUtil.d(TAG, "unRegisterFence() revert to ExtendAwarenessFence");
                    return this.mIAwarenessService.unRegisterExtendFence(callback, (ExtendAwarenessFence) awarenessFence, null, pendingOperation);
                }
                LogUtil.d(TAG, "unRegisterFence() is AwarenessFence");
                return this.mIAwarenessService.unRegisterFence(callback, awarenessFence, null, pendingOperation);
            } catch (RemoteException e) {
                LogUtil.e(TAG, "unRegisterFence() RemoteException");
                return false;
            }
        } else {
            LogUtil.e(TAG, "unRegisterFence() awarenessService is not connect");
            return false;
        }
    }

    private boolean registerFence(int fenceType, int fenceAction, IRequestCallBack callback, AwarenessFence awarenessFence, PendingIntent pendingOperation) {
        boolean isRegisterSuccess;
        if (!isConnectAwarenessService()) {
            LogUtil.e(TAG, "registerFence() awarenessService is not connect");
            return false;
        } else if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerFence() param error");
            return false;
        } else {
            LogUtil.d(TAG, "registerFence() pendingOperation.hashCode : " + pendingOperation.hashCode());
            try {
                awarenessFence.build(this.mContext);
                LogUtil.d(TAG, "registerFence() call binder awarenessFence :" + awarenessFence);
                switch (fenceType) {
                    case 6:
                        isRegisterSuccess = this.mIAwarenessService.registerLocationFence(callback, awarenessFence, null, pendingOperation);
                        break;
                    case 7:
                    default:
                        isRegisterSuccess = false;
                        break;
                    case 8:
                        isRegisterSuccess = this.mIAwarenessService.registerTimeFence(callback, awarenessFence, null, pendingOperation);
                        break;
                    case 9:
                        isRegisterSuccess = registerDeviceUseTypeFence(fenceAction, callback, awarenessFence, null, pendingOperation);
                        break;
                    case 10:
                        isRegisterSuccess = registerSystemEventTriggerTypeFence(fenceAction, callback, awarenessFence, null, pendingOperation);
                        break;
                }
                LogUtil.d(TAG, "registerFence() isRegisterSuccess :" + isRegisterSuccess);
                return isRegisterSuccess;
            } catch (RemoteException e) {
                LogUtil.e(TAG, "registerFence() RemoteException");
                return false;
            }
        }
    }

    private boolean registerDeviceUseTypeFence(int fenceAction, IRequestCallBack callback, AwarenessFence awarenessFence, Bundle bundle, PendingIntent pendingOperation) {
        boolean isRegisterSuccess;
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "registerDeviceUseTypeFence() mIAwarenessService = null");
            return false;
        }
        LogUtil.d(TAG, "registerDeviceUseTypeFence() fenceAction :" + fenceAction);
        if (isConnectAwarenessService()) {
            if (fenceAction == 1) {
                isRegisterSuccess = this.mIAwarenessService.registerAppUseTotalTimeFence(callback, awarenessFence, null, pendingOperation);
            } else if (fenceAction == 2) {
                isRegisterSuccess = this.mIAwarenessService.registerOneAppContinuousUseTimeFence(callback, awarenessFence, null, pendingOperation);
            } else if (fenceAction == 3) {
                isRegisterSuccess = this.mIAwarenessService.registerDeviceUseTotalTimeFence(callback, awarenessFence, null, pendingOperation);
            } else if (fenceAction != 4) {
                isRegisterSuccess = false;
            } else {
                try {
                    isRegisterSuccess = this.mIAwarenessService.registerScreenUnlockTotalNumberFence(callback, awarenessFence, null, pendingOperation);
                } catch (RemoteException e) {
                    LogUtil.e(TAG, "registerDeviceUseTypeFence() RemoteException");
                    return false;
                }
            }
            LogUtil.d(TAG, "registerDeviceUseTypeFence() isRegisterSuccess :" + isRegisterSuccess);
            return isRegisterSuccess;
        }
        LogUtil.e(TAG, "registerDeviceUseTypeFence() awarenessService is not connect");
        return false;
    }

    private boolean registerSystemEventTriggerTypeFence(int fenceAction, IRequestCallBack callback, AwarenessFence awarenessFence, Bundle bundle, PendingIntent pendingOperation) {
        boolean isRegisterSuccess;
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "registerSystemEventTriggerTypeFence() mIAwarenessService = null");
            return false;
        }
        LogUtil.d(TAG, "registerSystemEventTriggerTypeFence() fenceAction :" + fenceAction);
        if (isConnectAwarenessService()) {
            if (fenceAction != 1) {
                isRegisterSuccess = false;
            } else {
                try {
                    isRegisterSuccess = this.mIAwarenessService.registerScreenUnlockFence(callback, awarenessFence, null, pendingOperation);
                } catch (RemoteException e) {
                    LogUtil.e(TAG, "registerSystemEventTriggerTypeFence() RemoteException");
                    return false;
                }
            }
            LogUtil.d(TAG, "registerSystemEventTriggerTypeFence() isRegisterSuccess :" + isRegisterSuccess);
            return isRegisterSuccess;
        }
        LogUtil.e(TAG, "registerSystemEventTriggerTypeFence() awarenessService is not connect");
        return false;
    }

    private RequestResult getCurrentStatus(int type) {
        LogUtil.d(TAG, "getCurrentStatus() type = " + type);
        if (isConnectMsdpMovementServer()) {
            if (type != 1) {
                return null;
            }
            RequestResult result = buildRequestResultFromEvent(MovementController.getInstance().getMovementStatusEvent(), 2, -1);
            LogUtil.d(TAG, "getCurrentStatus() MOVEMENT_TYPE result : " + result);
            return result;
        } else if (isConnectAwarenessService()) {
            try {
                LogUtil.d(TAG, "getCurrentStatus() call binder");
                return this.mIAwarenessService.getCurrentStatus(type);
            } catch (RemoteException e) {
                LogUtil.e(TAG, "getCurrentStatus() RemoteException");
                return null;
            }
        } else {
            LogUtil.e(TAG, "getCurrentStatus() awarenessService is not connect");
            return null;
        }
    }

    public RequestResult getSupportAwarenessCapability(int type) {
        LogUtil.d(TAG, "getSupportAwarenessCapability() type = " + type);
        if (isConnectMsdpMovementServer()) {
            return AwarenessBinder.getInstance().getSupportAwarenessCapability(type);
        }
        if (isConnectAwarenessService()) {
            try {
                LogUtil.d(TAG, "getSupportAwarenessCapability() call binder");
                return this.mIAwarenessService.getSupportAwarenessCapability(type);
            } catch (RemoteException e) {
                LogUtil.e(TAG, "getSupportAwarenessCapability() RemoteException");
                RequestResult result = new RequestResult(AwarenessConstants.ERROR_REMOTE_CALLBACK_CODE, AwarenessConstants.ERROR_REMOTE_CALLBACK);
                result.setResultType(4);
                return result;
            }
        } else {
            LogUtil.e(TAG, "getSupportAwarenessCapability() awarenessService is not connect");
            RequestResult result2 = new RequestResult(AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED_CODE, AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED);
            result2.setResultType(4);
            return result2;
        }
    }

    public boolean registerMovementFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation) {
        if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerMovementFence() param error");
            return false;
        }
        LogUtil.d(TAG, "registerMovementFence() awarenessFence :" + awarenessFence);
        if (isConnectMsdpMovementServer()) {
            awarenessFence.build(this.mContext);
            LogUtil.d(TAG, "registerMovementFence() call local awarenessFence :" + awarenessFence);
            return AwarenessBinder.getInstance().registerMovementFence(callback, awarenessFence, null, pendingOperation);
        } else if (isConnectAwarenessService()) {
            try {
                awarenessFence.build(this.mContext);
                LogUtil.d(TAG, "registerMovementFence() call binder awarenessFence :" + awarenessFence);
                boolean isRegisterSuccess = this.mIAwarenessService.registerMovementFence(callback, awarenessFence, null, pendingOperation);
                LogUtil.d(TAG, "registerMovementFence()  isRegisterSuccess : " + isRegisterSuccess);
                return isRegisterSuccess;
            } catch (RemoteException e) {
                LogUtil.e(TAG, "registerMovementFence() RemoteException");
                return false;
            }
        } else {
            LogUtil.e(TAG, "registerMovementFence() awarenessService is not connect");
            return false;
        }
    }

    public boolean registerDeviceStatusFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation) {
        if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerDeviceStatusFence() param error");
            return false;
        }
        LogUtil.d(TAG, "registerDeviceStatusFence() awarenessFence :" + awarenessFence);
        if (isConnectAwarenessService()) {
            try {
                awarenessFence.build(this.mContext);
                LogUtil.d(TAG, "registerDeviceStatusFence() call binder awarenessFence :" + awarenessFence);
                boolean isRegisterSuccess = this.mIAwarenessService.registerDeviceStatusFence(callback, awarenessFence, null, pendingOperation);
                LogUtil.d(TAG, "registerDeviceStatusFence()  isRegisterSuccess : " + isRegisterSuccess);
                return isRegisterSuccess;
            } catch (RemoteException e) {
                LogUtil.e(TAG, "registerDeviceStatusFence() RemoteException");
                return false;
            }
        } else {
            LogUtil.e(TAG, "registerDeviceStatusFence() awarenessService is not connect");
            return false;
        }
    }

    public RequestResult setReportPeriod(ExtendAwarenessFence awarenessFence) {
        if (awarenessFence == null || awarenessFence.getRegisterBundle() == null) {
            LogUtil.e(TAG, "setReportPeriod(): illegal parameters!");
            RequestResult result = new RequestResult(AwarenessConstants.ERROR_PARAMETER_CODE, AwarenessConstants.ERROR_PARAMETER);
            result.setResultType(7);
            return result;
        }
        LogUtil.d(TAG, "setReportPeriod() awarenessFence :" + awarenessFence);
        if (isConnectMsdpMovementServer()) {
            if (awarenessFence.getType() == 1) {
                return MovementController.getInstance().doSetReportPeriod(awarenessFence);
            }
            LogUtil.e(TAG, "setReportPeriod(): illegal type parameter!");
            RequestResult result2 = new RequestResult(AwarenessConstants.ERROR_PARAMETER_CODE, AwarenessConstants.ERROR_PARAMETER);
            result2.setResultType(7);
            return result2;
        } else if (isConnectAwarenessService()) {
            try {
                LogUtil.d(TAG, "setReportPeriod() call binder");
                return this.mIAwarenessService.setReportPeriod(awarenessFence);
            } catch (RemoteException e) {
                LogUtil.e(TAG, "setReportPeriod() RemoteException");
                RequestResult result3 = new RequestResult(AwarenessConstants.ERROR_REMOTE_CALLBACK_CODE, AwarenessConstants.ERROR_REMOTE_CALLBACK);
                result3.setResultType(7);
                return result3;
            }
        } else {
            LogUtil.e(TAG, "setReportPeriod() awarenessService is not connect");
            RequestResult result4 = new RequestResult(AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED_CODE, AwarenessConstants.ERROR_SERVICE_NOT_CONNECTED);
            result4.setResultType(7);
            return result4;
        }
    }

    public boolean isIntegrateSensorHub() {
        LogUtil.d(TAG, "isIntegrateSensorHub()");
        if (isConnectMsdpMovementServer()) {
            return AwarenessBinder.getInstance().isIntegrateSensorHub();
        }
        if (isConnectAwarenessService()) {
            try {
                LogUtil.d(TAG, "isIntegrateSensorHub() call binder");
                return this.mIAwarenessService.isIntegrateSensorHub();
            } catch (RemoteException e) {
                LogUtil.e(TAG, "isIntegrateSensorHub() RemoteException");
                return false;
            }
        } else {
            LogUtil.e(TAG, "isIntegrateSensorHub() awarenessService is not connect");
            return false;
        }
    }

    private RequestResult buildRequestResultFromEvent(Event event, int resultType, int triggerStatus) {
        LogUtil.d(TAG, "buildRequestResultFromEvent() event :  " + event + " resultType : " + resultType + " triggerStatus : " + triggerStatus);
        if (event != null) {
            RequestResult result = new RequestResult(event.getEventCurType(), event.getEventCurStatus(), event.getEventCurAction(), null);
            result.setTime(event.getEventTime());
            result.setSensorTime(event.getEventSensorTime());
            result.setConfidence(event.getEventConfidence());
            result.setRegisterTopKey(null);
            result.setContent(null);
            result.setResultType(resultType);
            result.setTriggerStatus(triggerStatus);
            LogUtil.d(TAG, "buildRequestResultFromEvent() result : " + result);
            return result;
        }
        LogUtil.e(TAG, "buildRequestResultFromEvent() event == null ");
        return null;
    }

    public boolean registerAppLifeChangeFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "registerAppLifeChangeFence() mIAwarenessService = null");
            return false;
        } else if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerAppLifeChangeFence() param error");
            return false;
        } else {
            Bundle intentBundle = new Bundle();
            intentBundle.putParcelable(AwarenessConstants.REGISTER_APP_LIFE_FENCE_INTENT, intent);
            awarenessFence.setRegisterBundle(intentBundle);
            if (isConnectAwarenessService()) {
                try {
                    awarenessFence.build(this.mContext);
                    LogUtil.d(TAG, "registerAppLifeChangeFence() call binder awarenessFence :" + awarenessFence);
                    boolean isRegisterSuccess = this.mIAwarenessService.registerAppLifeChangeFence(callback, awarenessFence, null, pendingOperation);
                    LogUtil.d(TAG, "registerAppLifeChangeFence()  isRegisterSuccess : " + isRegisterSuccess);
                    return isRegisterSuccess;
                } catch (RemoteException e) {
                    LogUtil.e(TAG, "registerAppLifeChangeFence() RemoteException");
                    return false;
                }
            } else {
                LogUtil.e(TAG, "registerAppLifeChangeFence() awarenessService is not connect");
                return false;
            }
        }
    }

    public static boolean isAwarenessApkInstalled(Context context) {
        if (mIsAwarenessInstalled) {
            return true;
        }
        if (context == null) {
            LogUtil.e(TAG, "isAwarenessApkInstalled() context = null");
            return false;
        }
        mIsAwarenessInstalled = Utils.checkApkExist(context, "com.huawei.hiai");
        return mIsAwarenessInstalled;
    }

    public boolean registerSwingFaceRecognitionFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerSwingFaceRecognitionFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerSwingEyeGazeFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerSwingEyeGazeFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerSwingFaceDirectionFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerSwingFaceDirectionFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerSwingAgeEstimateFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerSwingAgeEstimateFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerSwingFaceDistanceFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerSwingFaceDistanceFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerSwingFatigueFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerSwingFatigueFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerSwingMotionGestureFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerSwingMotionGestureFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerSwingLyingFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerSwingLyingFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerSwingWalkingFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerSwingWalkingFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerAmbientLightFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerAmbientLightFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerSwingFaceNumChangeFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        return registerSwingFence("registerSwingFaceNumChangeFence", callback, awarenessFence, pendingOperation, intent);
    }

    public boolean registerSwingFence(String registerFenceName, IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, registerFenceName + " mIAwarenessService is null ,AwarenessService is not connect");
            return false;
        } else if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerSwingFence() param error");
            return false;
        } else {
            Bundle registerBundle = awarenessFence.getRegisterBundle();
            if (registerBundle == null) {
                registerBundle = new Bundle();
            }
            registerBundle.putParcelable(AwarenessConstants.REGISTER_SWING_FENCE_INTENT, intent);
            awarenessFence.setRegisterBundle(registerBundle);
            if (isConnectAwarenessService()) {
                try {
                    awarenessFence.build(this.mContext);
                    LogUtil.d(TAG, registerFenceName + " call binder awarenessFence :" + awarenessFence);
                    boolean isRegisterSuccess = this.mIAwarenessService.registerSwingFence(callback, awarenessFence, null, pendingOperation);
                    LogUtil.d(TAG, registerFenceName + " isRegisterSuccess : " + isRegisterSuccess);
                    return isRegisterSuccess;
                } catch (RemoteException e) {
                    LogUtil.e(TAG, registerFenceName + " RemoteException");
                    return false;
                }
            } else {
                LogUtil.e(TAG, "registerSwingFence() awarenessService is not connect");
                return false;
            }
        }
    }

    public int setSwingController(int controlCmd) {
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "setSwingController() mIAwarenessService is null ,AwarenessService is not connect");
            return 1;
        } else if (isConnectAwarenessService()) {
            try {
                int result = this.mIAwarenessService.setSwingController(controlCmd);
                LogUtil.d(TAG, "setSwingController() call binder result :" + result);
                return result;
            } catch (RemoteException e) {
                LogUtil.e(TAG, "setSwingController() RemoteException");
                return AwarenessConstants.ERROR_UNKNOWN_CODE;
            }
        } else {
            LogUtil.e(TAG, "setSwingController() awarenessService is not connect");
            return AwarenessConstants.ERROR_UNKNOWN_CODE;
        }
    }

    public boolean registerAwarenessListener(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, IAwarenessListener awarenessListener) {
        if (callback == null || awarenessFence == null || awarenessListener == null) {
            LogUtil.e(TAG, "registerAwarenessListener() param error");
            return false;
        }
        try {
            awarenessFence.build(this.mContext);
            if (isConnectAwarenessService()) {
                LogUtil.d(TAG, "registerAwarenessListener() call binder awarenessFence :" + awarenessFence);
                boolean isRegisterSuccess = this.mIAwarenessService.registerAwarenessListener(callback, awarenessFence, awarenessListener);
                LogUtil.d(TAG, "registerAwarenessListener() isRegisterSuccess : " + isRegisterSuccess);
                return isRegisterSuccess;
            }
            LogUtil.e(TAG, "registerAwarenessListener() awarenessService is not connect");
            return false;
        } catch (RemoteException e) {
            LogUtil.e(TAG, "registerAwarenessListener() RemoteException");
            return false;
        }
    }

    public boolean unRegisterAwarenessListener(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, IAwarenessListener awarenessListener) {
        if (callback == null || awarenessFence == null || awarenessListener == null) {
            LogUtil.e(TAG, "unRegisterAwarenessListener() param error");
            return false;
        }
        try {
            awarenessFence.build(this.mContext);
            if (isConnectAwarenessService()) {
                LogUtil.d(TAG, "unRegisterAwarenessListener() call binder awarenessFence :" + awarenessFence);
                boolean isUnregisterSuccess = this.mIAwarenessService.unRegisterAwarenessListener(callback, awarenessFence, awarenessListener);
                LogUtil.d(TAG, "unRegisterAwarenessListener() isUnregisterSuccess : " + isUnregisterSuccess);
                return isUnregisterSuccess;
            }
            LogUtil.e(TAG, "unRegisterAwarenessListener() awarenessService is not connect");
            return false;
        } catch (RemoteException e) {
            LogUtil.e(TAG, "unRegisterAwarenessListener() RemoteException");
            return false;
        }
    }

    public boolean setClientInfo(Bundle bundle) {
        LogUtil.d(TAG, "setClientInfo()");
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "setClientInfo() mIAwarenessService is null ,AwarenessService is not connect");
            return false;
        } else if (this.mContext == null) {
            LogUtil.e(TAG, "setClientInfo() mContext == null");
            return false;
        } else if (isConnectAwarenessService()) {
            try {
                LogUtil.d(TAG, "setClientInfo() call binder");
                return this.mIAwarenessService.setClientInfo(this.mContext.getPackageName(), bundle);
            } catch (RemoteException e) {
                LogUtil.e(TAG, "setClientInfo() RemoteException");
                return false;
            }
        } else {
            LogUtil.e(TAG, "setClientInfo() awarenessService is not connect");
            return false;
        }
    }

    public boolean registerMapInfoReportFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "registerMapInfoReportFence() mIAwarenessService = null");
            return false;
        } else if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerMapInfoReportFence() param error");
            return false;
        } else {
            Bundle registerBundle = awarenessFence.getRegisterBundle();
            if (registerBundle == null) {
                registerBundle = new Bundle();
            }
            registerBundle.putParcelable(AwarenessConstants.MapInfoFenceConstants.REGISTER_FENCE_INTENT, intent);
            awarenessFence.setRegisterBundle(registerBundle);
            if (isConnectAwarenessService()) {
                try {
                    awarenessFence.build(this.mContext);
                    LogUtil.d(TAG, "registerMapInfoReportFence() call binder awarenessFence :" + awarenessFence);
                    boolean isRegisterSuccess = this.mIAwarenessService.registerMapInfoReportFence(callback, awarenessFence, null, pendingOperation);
                    LogUtil.d(TAG, "registerMapInfoReportFence() isRegisterSuccess : " + isRegisterSuccess);
                    return isRegisterSuccess;
                } catch (RemoteException e) {
                    LogUtil.e(TAG, "registerMapInfoReportFence() RemoteException");
                    return false;
                }
            } else {
                LogUtil.e(TAG, "registerMapInfoReportFence() awarenessService is not connect");
                return false;
            }
        }
    }

    public boolean registerDatabaseMonitorFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        if (this.mIAwarenessService == null) {
            LogUtil.e(TAG, "registerDatabaseMonitorFence() mIAwarenessService = null");
            return false;
        } else if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerDatabaseMonitorFence() param error");
            return false;
        } else {
            Bundle registerBundle = awarenessFence.getRegisterBundle();
            if (registerBundle == null) {
                registerBundle = new Bundle();
            }
            registerBundle.putParcelable(AwarenessConstants.DataBaseFenceConstants.REGISTER_DATA_BASE_CHANGE_FENCE_INTENT, intent);
            if (isConnectAwarenessService()) {
                try {
                    awarenessFence.build(this.mContext);
                    LogUtil.d(TAG, "registerDatabaseMonitorFence() call binder awarenessFence :" + awarenessFence);
                    boolean isRegisterSuccess = this.mIAwarenessService.registerDatabaseMonitorFence(callback, awarenessFence, null, pendingOperation);
                    LogUtil.d(TAG, "registerDatabaseMonitorFence()  isRegisterSuccess : " + isRegisterSuccess);
                    return isRegisterSuccess;
                } catch (RemoteException e) {
                    LogUtil.e(TAG, "registerDatabaseMonitorFence() RemoteException");
                    return false;
                }
            } else {
                LogUtil.e(TAG, "registerDatabaseMonitorFence() awarenessService is not connect");
                return false;
            }
        }
    }

    public boolean registerAwarenessFence(IRequestCallBack callback, ExtendAwarenessFence awarenessFence, PendingIntent pendingOperation, Intent intent) {
        if (callback == null || awarenessFence == null || pendingOperation == null) {
            LogUtil.e(TAG, "registerAwarenessFence() param error");
            return false;
        }
        LogUtil.d(TAG, "registerAwarenessFence() awarenessFence :" + awarenessFence);
        if (isConnectAwarenessService()) {
            try {
                awarenessFence.build(this.mContext);
                LogUtil.d(TAG, "registerAwarenessFence() call binder awarenessFence :" + awarenessFence);
                boolean isRegisterSuccess = this.mIAwarenessService.registerDeviceStatusFence(callback, awarenessFence, null, pendingOperation);
                LogUtil.d(TAG, "registerAwarenessFence()  isRegisterSuccess : " + isRegisterSuccess);
                return isRegisterSuccess;
            } catch (RemoteException e) {
                LogUtil.e(TAG, "registerAwarenessFence() RemoteException");
                return false;
            }
        } else {
            LogUtil.e(TAG, "registerAwarenessFence() awarenessService is not connect");
            return false;
        }
    }

    public String getFenceKey(int type, int status, int action, String secondAction) {
        if (type == 0 || status == -1 || action == -1) {
            return "";
        }
        if (TextUtils.isEmpty(secondAction)) {
            return type + "," + status + "," + action;
        }
        return type + "," + status + "," + action + AwarenessConstants.SECOND_ACTION_SPLITE_TAG + secondAction;
    }

    private boolean isConnectMsdpMovementServer() {
        boolean isConMovementServer = ConnectServiceManager.getInstance().isConnectMsdpMovementServer();
        LogUtil.d(TAG, "isConnectMsdpMovementServer() isConMovementServer:" + isConMovementServer);
        return isConMovementServer;
    }

    private boolean isConnectAwarenessService() {
        LogUtil.d(TAG, "isConnectAwarenessService() mIsConnectAwarenessService:" + this.mIsConnectAwarenessService);
        return this.mIsConnectAwarenessService;
    }
}
