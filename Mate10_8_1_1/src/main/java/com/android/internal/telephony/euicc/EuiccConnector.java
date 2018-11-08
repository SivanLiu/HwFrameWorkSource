package com.android.internal.telephony.euicc;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.service.euicc.GetDefaultDownloadableSubscriptionListResult;
import android.service.euicc.GetDownloadableSubscriptionMetadataResult;
import android.service.euicc.GetEuiccProfileInfoListResult;
import android.service.euicc.IDeleteSubscriptionCallback;
import android.service.euicc.IDownloadSubscriptionCallback;
import android.service.euicc.IEraseSubscriptionsCallback;
import android.service.euicc.IEuiccService;
import android.service.euicc.IGetDefaultDownloadableSubscriptionListCallback;
import android.service.euicc.IGetDownloadableSubscriptionMetadataCallback;
import android.service.euicc.IGetEidCallback.Stub;
import android.service.euicc.IGetEuiccInfoCallback;
import android.service.euicc.IGetEuiccProfileInfoListCallback;
import android.service.euicc.IRetainSubscriptionsForFactoryResetCallback;
import android.service.euicc.ISwitchToSubscriptionCallback;
import android.service.euicc.IUpdateSubscriptionNicknameCallback;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccInfo;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.content.PackageMonitor;
import com.android.internal.util.IState;
import com.android.internal.util.State;
import com.android.internal.util.StateMachine;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class EuiccConnector extends StateMachine implements ServiceConnection {
    private static final int BIND_TIMEOUT_MILLIS = 30000;
    private static final int CMD_COMMAND_COMPLETE = 6;
    private static final int CMD_CONNECT_TIMEOUT = 2;
    private static final int CMD_DELETE_SUBSCRIPTION = 106;
    private static final int CMD_DOWNLOAD_SUBSCRIPTION = 102;
    private static final int CMD_ERASE_SUBSCRIPTIONS = 109;
    private static final int CMD_GET_DEFAULT_DOWNLOADABLE_SUBSCRIPTION_LIST = 104;
    private static final int CMD_GET_DOWNLOADABLE_SUBSCRIPTION_METADATA = 101;
    private static final int CMD_GET_EID = 100;
    private static final int CMD_GET_EUICC_INFO = 105;
    private static final int CMD_GET_EUICC_PROFILE_INFO_LIST = 103;
    private static final int CMD_LINGER_TIMEOUT = 3;
    private static final int CMD_PACKAGE_CHANGE = 1;
    private static final int CMD_RETAIN_SUBSCRIPTIONS = 110;
    private static final int CMD_SERVICE_CONNECTED = 4;
    private static final int CMD_SERVICE_DISCONNECTED = 5;
    private static final int CMD_SWITCH_TO_SUBSCRIPTION = 107;
    private static final int CMD_UPDATE_SUBSCRIPTION_NICKNAME = 108;
    private static final int EUICC_QUERY_FLAGS = 269484096;
    static final int LINGER_TIMEOUT_MILLIS = 60000;
    private static final String TAG = "EuiccConnector";
    private Set<BaseEuiccCommandCallback> mActiveCommandCallbacks = new ArraySet();
    public AvailableState mAvailableState;
    public BindingState mBindingState;
    public ConnectedState mConnectedState;
    private Context mContext;
    public DisconnectedState mDisconnectedState;
    private IEuiccService mEuiccService;
    private final PackageMonitor mPackageMonitor = new EuiccPackageMonitor();
    private PackageManager mPm;
    private ServiceInfo mSelectedComponent;
    public UnavailableState mUnavailableState;
    private final BroadcastReceiver mUserUnlockedReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            if ("android.intent.action.USER_UNLOCKED".equals(intent.getAction())) {
                EuiccConnector.this.sendMessage(1);
            }
        }
    };

    private class AvailableState extends State {
        private AvailableState() {
        }

        public boolean processMessage(Message message) {
            if (!EuiccConnector.isEuiccCommand(message.what)) {
                return false;
            }
            EuiccConnector.this.deferMessage(message);
            EuiccConnector.this.transitionTo(EuiccConnector.this.mBindingState);
            return true;
        }
    }

    public interface BaseEuiccCommandCallback {
        void onEuiccServiceUnavailable();
    }

    private class BindingState extends State {
        private BindingState() {
        }

        public void enter() {
            if (EuiccConnector.this.createBinding()) {
                EuiccConnector.this.transitionTo(EuiccConnector.this.mDisconnectedState);
            } else {
                EuiccConnector.this.transitionTo(EuiccConnector.this.mAvailableState);
            }
        }

        public boolean processMessage(Message message) {
            EuiccConnector.this.deferMessage(message);
            return true;
        }
    }

    private class ConnectedState extends State {
        private ConnectedState() {
        }

        public void enter() {
            EuiccConnector.this.removeMessages(2);
            EuiccConnector.this.sendMessageDelayed(3, 60000);
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean processMessage(Message message) {
            if (message.what == 5) {
                EuiccConnector.this.mEuiccService = null;
                EuiccConnector.this.transitionTo(EuiccConnector.this.mDisconnectedState);
                return true;
            } else if (message.what == 3) {
                EuiccConnector.this.unbind();
                EuiccConnector.this.transitionTo(EuiccConnector.this.mAvailableState);
                return true;
            } else if (message.what == 6) {
                message.obj.run();
                return true;
            } else if (!EuiccConnector.isEuiccCommand(message.what)) {
                return false;
            } else {
                final BaseEuiccCommandCallback callback = EuiccConnector.getCallback(message);
                EuiccConnector.this.onCommandStart(callback);
                try {
                    switch (message.what) {
                        case 100:
                            EuiccConnector.this.mEuiccService.getEid(-1, new Stub() {
                                public void onSuccess(String eid) {
                                    EuiccConnector.this.sendMessage(6, new -$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY((byte) 0, this, callback, eid));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$1_27371(BaseEuiccCommandCallback callback, String eid) {
                                    ((GetEidCommandCallback) callback).onGetEidComplete(eid);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 101:
                            GetMetadataRequest request = message.obj;
                            EuiccConnector.this.mEuiccService.getDownloadableSubscriptionMetadata(-1, request.mSubscription, request.mForceDeactivateSim, new IGetDownloadableSubscriptionMetadataCallback.Stub() {
                                public void onComplete(GetDownloadableSubscriptionMetadataResult result) {
                                    EuiccConnector.this.sendMessage(6, new -$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY((byte) 1, this, callback, result));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$2_28578(BaseEuiccCommandCallback callback, GetDownloadableSubscriptionMetadataResult result) {
                                    ((GetMetadataCommandCallback) callback).onGetMetadataComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 102:
                            DownloadRequest request2 = message.obj;
                            EuiccConnector.this.mEuiccService.downloadSubscription(-1, request2.mSubscription, request2.mSwitchAfterDownload, request2.mForceDeactivateSim, new IDownloadSubscriptionCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new com.android.internal.telephony.euicc.-$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY.AnonymousClass1((byte) 2, result, this, callback));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$3_29724(BaseEuiccCommandCallback callback, int result) {
                                    ((DownloadCommandCallback) callback).onDownloadComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case EuiccConnector.CMD_GET_EUICC_PROFILE_INFO_LIST /*103*/:
                            EuiccConnector.this.mEuiccService.getEuiccProfileInfoList(-1, new IGetEuiccProfileInfoListCallback.Stub() {
                                public void onComplete(GetEuiccProfileInfoListResult result) {
                                    EuiccConnector.this.sendMessage(6, new -$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY((byte) 2, this, callback, result));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$4_30676(BaseEuiccCommandCallback callback, GetEuiccProfileInfoListResult result) {
                                    ((GetEuiccProfileInfoListCommandCallback) callback).onListComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 104:
                            EuiccConnector.this.mEuiccService.getDefaultDownloadableSubscriptionList(-1, message.obj.mForceDeactivateSim, new IGetDefaultDownloadableSubscriptionListCallback.Stub() {
                                public void onComplete(GetDefaultDownloadableSubscriptionListResult result) {
                                    EuiccConnector.this.sendMessage(6, new -$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY((byte) 3, this, callback, result));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$5_31902(BaseEuiccCommandCallback callback, GetDefaultDownloadableSubscriptionListResult result) {
                                    ((GetDefaultListCommandCallback) callback).onGetDefaultListComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 105:
                            EuiccConnector.this.mEuiccService.getEuiccInfo(-1, new IGetEuiccInfoCallback.Stub() {
                                public void onSuccess(EuiccInfo euiccInfo) {
                                    EuiccConnector.this.sendMessage(6, new -$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY((byte) 4, this, callback, euiccInfo));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$6_32764(BaseEuiccCommandCallback callback, EuiccInfo euiccInfo) {
                                    ((GetEuiccInfoCommandCallback) callback).onGetEuiccInfoComplete(euiccInfo);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 106:
                            EuiccConnector.this.mEuiccService.deleteSubscription(-1, message.obj.mIccid, new IDeleteSubscriptionCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new com.android.internal.telephony.euicc.-$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY.AnonymousClass1((byte) 3, result, this, callback));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$7_33731(BaseEuiccCommandCallback callback, int result) {
                                    ((DeleteCommandCallback) callback).onDeleteComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case EuiccConnector.CMD_SWITCH_TO_SUBSCRIPTION /*107*/:
                            SwitchRequest request3 = message.obj;
                            EuiccConnector.this.mEuiccService.switchToSubscription(-1, request3.mIccid, request3.mForceDeactivateSim, new ISwitchToSubscriptionCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new com.android.internal.telephony.euicc.-$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY.AnonymousClass1((byte) 4, result, this, callback));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$8_34755(BaseEuiccCommandCallback callback, int result) {
                                    ((SwitchCommandCallback) callback).onSwitchComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 108:
                            UpdateNicknameRequest request4 = message.obj;
                            EuiccConnector.this.mEuiccService.updateSubscriptionNickname(-1, request4.mIccid, request4.mNickname, new IUpdateSubscriptionNicknameCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new com.android.internal.telephony.euicc.-$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY.AnonymousClass1((byte) 5, result, this, callback));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$9_35803(BaseEuiccCommandCallback callback, int result) {
                                    ((UpdateNicknameCommandCallback) callback).onUpdateNicknameComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case EuiccConnector.CMD_ERASE_SUBSCRIPTIONS /*109*/:
                            EuiccConnector.this.mEuiccService.eraseSubscriptions(-1, new IEraseSubscriptionsCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new com.android.internal.telephony.euicc.-$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY.AnonymousClass1((byte) 0, result, this, callback));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$10_36674(BaseEuiccCommandCallback callback, int result) {
                                    ((EraseCommandCallback) callback).onEraseComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case EuiccConnector.CMD_RETAIN_SUBSCRIPTIONS /*110*/:
                            EuiccConnector.this.mEuiccService.retainSubscriptionsForFactoryReset(-1, new IRetainSubscriptionsForFactoryResetCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new com.android.internal.telephony.euicc.-$Lambda$XGQhG5dbeONW7ErYP0MG74e0DLY.AnonymousClass1((byte) 1, result, this, callback));
                                }

                                /* synthetic */ void lambda$-com_android_internal_telephony_euicc_EuiccConnector$ConnectedState$11_37560(BaseEuiccCommandCallback callback, int result) {
                                    ((RetainSubscriptionsCommandCallback) callback).onRetainSubscriptionsComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        default:
                            Log.wtf(EuiccConnector.TAG, "Unimplemented eUICC command: " + message.what);
                            callback.onEuiccServiceUnavailable();
                            EuiccConnector.this.onCommandEnd(callback);
                            return true;
                    }
                } catch (Exception e) {
                    Log.w(EuiccConnector.TAG, "Exception making binder call to EuiccService", e);
                    callback.onEuiccServiceUnavailable();
                    EuiccConnector.this.onCommandEnd(callback);
                }
                return true;
            }
        }

        public void exit() {
            EuiccConnector.this.removeMessages(3);
            for (BaseEuiccCommandCallback callback : EuiccConnector.this.mActiveCommandCallbacks) {
                callback.onEuiccServiceUnavailable();
            }
            EuiccConnector.this.mActiveCommandCallbacks.clear();
        }
    }

    public interface DeleteCommandCallback extends BaseEuiccCommandCallback {
        void onDeleteComplete(int i);
    }

    static class DeleteRequest {
        DeleteCommandCallback mCallback;
        String mIccid;

        DeleteRequest() {
        }
    }

    private class DisconnectedState extends State {
        private DisconnectedState() {
        }

        public void enter() {
            EuiccConnector.this.sendMessageDelayed(2, 30000);
        }

        public boolean processMessage(Message message) {
            if (message.what == 4) {
                EuiccConnector.this.mEuiccService = (IEuiccService) message.obj;
                EuiccConnector.this.transitionTo(EuiccConnector.this.mConnectedState);
                return true;
            } else if (message.what == 1) {
                ServiceInfo bestComponent = EuiccConnector.this.findBestComponent();
                String affectedPackage = message.obj;
                boolean equals = bestComponent == null ? EuiccConnector.this.mSelectedComponent != null : EuiccConnector.this.mSelectedComponent != null ? Objects.equals(bestComponent.getComponentName(), EuiccConnector.this.mSelectedComponent.getComponentName()) : true;
                boolean equals2;
                if (bestComponent != null) {
                    equals2 = Objects.equals(bestComponent.packageName, affectedPackage);
                } else {
                    equals2 = false;
                }
                if (!equals || r2) {
                    EuiccConnector.this.unbind();
                    EuiccConnector.this.mSelectedComponent = bestComponent;
                    if (EuiccConnector.this.mSelectedComponent == null) {
                        EuiccConnector.this.transitionTo(EuiccConnector.this.mUnavailableState);
                    } else {
                        EuiccConnector.this.transitionTo(EuiccConnector.this.mBindingState);
                    }
                }
                return true;
            } else if (message.what == 2) {
                EuiccConnector.this.transitionTo(EuiccConnector.this.mAvailableState);
                return true;
            } else if (!EuiccConnector.isEuiccCommand(message.what)) {
                return false;
            } else {
                EuiccConnector.this.deferMessage(message);
                return true;
            }
        }
    }

    public interface DownloadCommandCallback extends BaseEuiccCommandCallback {
        void onDownloadComplete(int i);
    }

    static class DownloadRequest {
        DownloadCommandCallback mCallback;
        boolean mForceDeactivateSim;
        DownloadableSubscription mSubscription;
        boolean mSwitchAfterDownload;

        DownloadRequest() {
        }
    }

    public interface EraseCommandCallback extends BaseEuiccCommandCallback {
        void onEraseComplete(int i);
    }

    private class EuiccPackageMonitor extends PackageMonitor {
        private EuiccPackageMonitor() {
        }

        public void onPackageAdded(String packageName, int reason) {
            sendPackageChange(packageName, true);
        }

        public void onPackageRemoved(String packageName, int reason) {
            sendPackageChange(packageName, true);
        }

        public void onPackageUpdateFinished(String packageName, int uid) {
            sendPackageChange(packageName, true);
        }

        public void onPackageModified(String packageName) {
            sendPackageChange(packageName, false);
        }

        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            if (doit) {
                for (String packageName : packages) {
                    sendPackageChange(packageName, true);
                }
            }
            return super.onHandleForceStop(intent, packages, uid, doit);
        }

        private void sendPackageChange(String packageName, boolean forceUnbindForThisPackage) {
            Object packageName2;
            EuiccConnector euiccConnector = EuiccConnector.this;
            if (!forceUnbindForThisPackage) {
                packageName2 = null;
            }
            euiccConnector.sendMessage(1, packageName2);
        }
    }

    public interface GetDefaultListCommandCallback extends BaseEuiccCommandCallback {
        void onGetDefaultListComplete(GetDefaultDownloadableSubscriptionListResult getDefaultDownloadableSubscriptionListResult);
    }

    static class GetDefaultListRequest {
        GetDefaultListCommandCallback mCallback;
        boolean mForceDeactivateSim;

        GetDefaultListRequest() {
        }
    }

    public interface GetEidCommandCallback extends BaseEuiccCommandCallback {
        void onGetEidComplete(String str);
    }

    public interface GetEuiccInfoCommandCallback extends BaseEuiccCommandCallback {
        void onGetEuiccInfoComplete(EuiccInfo euiccInfo);
    }

    interface GetEuiccProfileInfoListCommandCallback extends BaseEuiccCommandCallback {
        void onListComplete(GetEuiccProfileInfoListResult getEuiccProfileInfoListResult);
    }

    public interface GetMetadataCommandCallback extends BaseEuiccCommandCallback {
        void onGetMetadataComplete(GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResult);
    }

    static class GetMetadataRequest {
        GetMetadataCommandCallback mCallback;
        boolean mForceDeactivateSim;
        DownloadableSubscription mSubscription;

        GetMetadataRequest() {
        }
    }

    public interface RetainSubscriptionsCommandCallback extends BaseEuiccCommandCallback {
        void onRetainSubscriptionsComplete(int i);
    }

    public interface SwitchCommandCallback extends BaseEuiccCommandCallback {
        void onSwitchComplete(int i);
    }

    static class SwitchRequest {
        SwitchCommandCallback mCallback;
        boolean mForceDeactivateSim;
        String mIccid;

        SwitchRequest() {
        }
    }

    private class UnavailableState extends State {
        private UnavailableState() {
        }

        public boolean processMessage(Message message) {
            if (message.what == 1) {
                EuiccConnector.this.mSelectedComponent = EuiccConnector.this.findBestComponent();
                if (EuiccConnector.this.mSelectedComponent != null) {
                    EuiccConnector.this.transitionTo(EuiccConnector.this.mAvailableState);
                } else if (EuiccConnector.this.getCurrentState() != EuiccConnector.this.mUnavailableState) {
                    EuiccConnector.this.transitionTo(EuiccConnector.this.mUnavailableState);
                }
                return true;
            } else if (!EuiccConnector.isEuiccCommand(message.what)) {
                return false;
            } else {
                EuiccConnector.getCallback(message).onEuiccServiceUnavailable();
                return true;
            }
        }
    }

    public interface UpdateNicknameCommandCallback extends BaseEuiccCommandCallback {
        void onUpdateNicknameComplete(int i);
    }

    static class UpdateNicknameRequest {
        UpdateNicknameCommandCallback mCallback;
        String mIccid;
        String mNickname;

        UpdateNicknameRequest() {
        }
    }

    private static boolean isEuiccCommand(int what) {
        return what >= 100;
    }

    public static ActivityInfo findBestActivity(PackageManager packageManager, Intent intent) {
        ActivityInfo bestComponent = (ActivityInfo) findBestComponent(packageManager, packageManager.queryIntentActivities(intent, EUICC_QUERY_FLAGS));
        if (bestComponent == null) {
            Log.w(TAG, "No valid component found for intent: " + intent);
        }
        return bestComponent;
    }

    public static ComponentInfo findBestComponent(PackageManager packageManager) {
        ComponentInfo bestComponent = findBestComponent(packageManager, packageManager.queryIntentServices(new Intent("android.service.euicc.EuiccService"), EUICC_QUERY_FLAGS));
        if (bestComponent == null) {
            Log.w(TAG, "No valid EuiccService implementation found");
        }
        return bestComponent;
    }

    EuiccConnector(Context context) {
        super(TAG);
        init(context);
    }

    public EuiccConnector(Context context, Looper looper) {
        super(TAG, looper);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        this.mUnavailableState = new UnavailableState();
        addState(this.mUnavailableState);
        this.mAvailableState = new AvailableState();
        addState(this.mAvailableState, this.mUnavailableState);
        this.mBindingState = new BindingState();
        addState(this.mBindingState);
        this.mDisconnectedState = new DisconnectedState();
        addState(this.mDisconnectedState);
        this.mConnectedState = new ConnectedState();
        addState(this.mConnectedState, this.mDisconnectedState);
        this.mSelectedComponent = findBestComponent();
        setInitialState(this.mSelectedComponent != null ? this.mAvailableState : this.mUnavailableState);
        this.mPackageMonitor.register(this.mContext, null, false);
        this.mContext.registerReceiver(this.mUserUnlockedReceiver, new IntentFilter("android.intent.action.USER_UNLOCKED"));
        start();
    }

    public void onHalting() {
        this.mPackageMonitor.unregister();
        this.mContext.unregisterReceiver(this.mUserUnlockedReceiver);
    }

    public void getEid(GetEidCommandCallback callback) {
        sendMessage(100, callback);
    }

    public void getDownloadableSubscriptionMetadata(DownloadableSubscription subscription, boolean forceDeactivateSim, GetMetadataCommandCallback callback) {
        GetMetadataRequest request = new GetMetadataRequest();
        request.mSubscription = subscription;
        request.mForceDeactivateSim = forceDeactivateSim;
        request.mCallback = callback;
        sendMessage(101, request);
    }

    public void downloadSubscription(DownloadableSubscription subscription, boolean switchAfterDownload, boolean forceDeactivateSim, DownloadCommandCallback callback) {
        DownloadRequest request = new DownloadRequest();
        request.mSubscription = subscription;
        request.mSwitchAfterDownload = switchAfterDownload;
        request.mForceDeactivateSim = forceDeactivateSim;
        request.mCallback = callback;
        sendMessage(102, request);
    }

    void getEuiccProfileInfoList(GetEuiccProfileInfoListCommandCallback callback) {
        sendMessage(CMD_GET_EUICC_PROFILE_INFO_LIST, callback);
    }

    public void getDefaultDownloadableSubscriptionList(boolean forceDeactivateSim, GetDefaultListCommandCallback callback) {
        GetDefaultListRequest request = new GetDefaultListRequest();
        request.mForceDeactivateSim = forceDeactivateSim;
        request.mCallback = callback;
        sendMessage(104, request);
    }

    public void getEuiccInfo(GetEuiccInfoCommandCallback callback) {
        sendMessage(105, callback);
    }

    public void deleteSubscription(String iccid, DeleteCommandCallback callback) {
        DeleteRequest request = new DeleteRequest();
        request.mIccid = iccid;
        request.mCallback = callback;
        sendMessage(106, request);
    }

    public void switchToSubscription(String iccid, boolean forceDeactivateSim, SwitchCommandCallback callback) {
        SwitchRequest request = new SwitchRequest();
        request.mIccid = iccid;
        request.mForceDeactivateSim = forceDeactivateSim;
        request.mCallback = callback;
        sendMessage(CMD_SWITCH_TO_SUBSCRIPTION, request);
    }

    public void updateSubscriptionNickname(String iccid, String nickname, UpdateNicknameCommandCallback callback) {
        UpdateNicknameRequest request = new UpdateNicknameRequest();
        request.mIccid = iccid;
        request.mNickname = nickname;
        request.mCallback = callback;
        sendMessage(108, request);
    }

    public void eraseSubscriptions(EraseCommandCallback callback) {
        sendMessage(CMD_ERASE_SUBSCRIPTIONS, callback);
    }

    public void retainSubscriptions(RetainSubscriptionsCommandCallback callback) {
        sendMessage(CMD_RETAIN_SUBSCRIPTIONS, callback);
    }

    private static BaseEuiccCommandCallback getCallback(Message message) {
        switch (message.what) {
            case 100:
            case CMD_GET_EUICC_PROFILE_INFO_LIST /*103*/:
            case 105:
            case CMD_ERASE_SUBSCRIPTIONS /*109*/:
            case CMD_RETAIN_SUBSCRIPTIONS /*110*/:
                return (BaseEuiccCommandCallback) message.obj;
            case 101:
                return ((GetMetadataRequest) message.obj).mCallback;
            case 102:
                return ((DownloadRequest) message.obj).mCallback;
            case 104:
                return ((GetDefaultListRequest) message.obj).mCallback;
            case 106:
                return ((DeleteRequest) message.obj).mCallback;
            case CMD_SWITCH_TO_SUBSCRIPTION /*107*/:
                return ((SwitchRequest) message.obj).mCallback;
            case 108:
                return ((UpdateNicknameRequest) message.obj).mCallback;
            default:
                throw new IllegalArgumentException("Unsupported message: " + message.what);
        }
    }

    private void onCommandStart(BaseEuiccCommandCallback callback) {
        this.mActiveCommandCallbacks.add(callback);
        removeMessages(3);
    }

    private void onCommandEnd(BaseEuiccCommandCallback callback) {
        if (!this.mActiveCommandCallbacks.remove(callback)) {
            Log.wtf(TAG, "Callback already removed from mActiveCommandCallbacks");
        }
        if (this.mActiveCommandCallbacks.isEmpty()) {
            sendMessageDelayed(3, 60000);
        }
    }

    private ServiceInfo findBestComponent() {
        return (ServiceInfo) findBestComponent(this.mPm);
    }

    private boolean createBinding() {
        if (this.mSelectedComponent == null) {
            Log.wtf(TAG, "Attempting to create binding but no component is selected");
            return false;
        }
        Intent intent = new Intent("android.service.euicc.EuiccService");
        intent.setComponent(this.mSelectedComponent.getComponentName());
        return this.mContext.bindService(intent, this, 67108865);
    }

    private void unbind() {
        this.mEuiccService = null;
        this.mContext.unbindService(this);
    }

    private static ComponentInfo findBestComponent(PackageManager packageManager, List<ResolveInfo> resolveInfoList) {
        int bestPriority = Integer.MIN_VALUE;
        ComponentInfo bestComponent = null;
        if (resolveInfoList != null) {
            for (ResolveInfo resolveInfo : resolveInfoList) {
                if (isValidEuiccComponent(packageManager, resolveInfo) && resolveInfo.filter.getPriority() > bestPriority) {
                    bestPriority = resolveInfo.filter.getPriority();
                    bestComponent = resolveInfo.getComponentInfo();
                }
            }
        }
        return bestComponent;
    }

    private static boolean isValidEuiccComponent(PackageManager packageManager, ResolveInfo resolveInfo) {
        ComponentInfo componentInfo = resolveInfo.getComponentInfo();
        String packageName = componentInfo.getComponentName().getPackageName();
        if (packageManager.checkPermission("com.android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS", packageName) != 0) {
            Log.wtf(TAG, "Package " + packageName + " does not declare WRITE_EMBEDDED_SUBSCRIPTIONS");
            return false;
        }
        String permission;
        if (componentInfo instanceof ServiceInfo) {
            permission = ((ServiceInfo) componentInfo).permission;
        } else if (componentInfo instanceof ActivityInfo) {
            permission = ((ActivityInfo) componentInfo).permission;
        } else {
            throw new IllegalArgumentException("Can only verify services/activities");
        }
        if (!TextUtils.equals(permission, "com.android.permission.BIND_EUICC_SERVICE")) {
            Log.wtf(TAG, "Package " + packageName + " does not require the BIND_EUICC_SERVICE permission");
            return false;
        } else if (resolveInfo.filter != null && resolveInfo.filter.getPriority() != 0) {
            return true;
        } else {
            Log.wtf(TAG, "Package " + packageName + " does not specify a priority");
            return false;
        }
    }

    public void onServiceConnected(ComponentName name, IBinder service) {
        sendMessage(4, IEuiccService.Stub.asInterface(service));
    }

    public void onServiceDisconnected(ComponentName name) {
        sendMessage(5);
    }

    protected void unhandledMessage(Message msg) {
        IState state = getCurrentState();
        Log.wtf(TAG, "Unhandled message " + msg.what + " in state " + (state == null ? "null" : state.getName()));
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println("mSelectedComponent=" + this.mSelectedComponent);
        pw.println("mEuiccService=" + this.mEuiccService);
        pw.println("mActiveCommandCount=" + this.mActiveCommandCallbacks.size());
    }
}
