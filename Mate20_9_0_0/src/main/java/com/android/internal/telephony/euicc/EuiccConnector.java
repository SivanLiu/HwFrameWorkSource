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
import android.os.RemoteException;
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
import android.service.euicc.IGetOtaStatusCallback;
import android.service.euicc.IOtaStatusChangedCallback;
import android.service.euicc.IRetainSubscriptionsForFactoryResetCallback;
import android.service.euicc.ISwitchToSubscriptionCallback;
import android.service.euicc.IUpdateSubscriptionNicknameCallback;
import android.telephony.euicc.DownloadableSubscription;
import android.telephony.euicc.EuiccInfo;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.Log;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.annotations.VisibleForTesting.Visibility;
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
    private static final int CMD_GET_OTA_STATUS = 111;
    private static final int CMD_LINGER_TIMEOUT = 3;
    private static final int CMD_PACKAGE_CHANGE = 1;
    private static final int CMD_RETAIN_SUBSCRIPTIONS = 110;
    private static final int CMD_SERVICE_CONNECTED = 4;
    private static final int CMD_SERVICE_DISCONNECTED = 5;
    private static final int CMD_START_OTA_IF_NECESSARY = 112;
    private static final int CMD_SWITCH_TO_SUBSCRIPTION = 107;
    private static final int CMD_UPDATE_SUBSCRIPTION_NICKNAME = 108;
    private static final int EUICC_QUERY_FLAGS = 269484096;
    @VisibleForTesting
    static final int LINGER_TIMEOUT_MILLIS = 60000;
    private static final String TAG = "EuiccConnector";
    private Set<BaseEuiccCommandCallback> mActiveCommandCallbacks = new ArraySet();
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public AvailableState mAvailableState;
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public BindingState mBindingState;
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public ConnectedState mConnectedState;
    private Context mContext;
    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public DisconnectedState mDisconnectedState;
    private IEuiccService mEuiccService;
    private final PackageMonitor mPackageMonitor = new EuiccPackageMonitor(this, null);
    private PackageManager mPm;
    private ServiceInfo mSelectedComponent;
    @VisibleForTesting(visibility = Visibility.PACKAGE)
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

        /* synthetic */ AvailableState(EuiccConnector x0, AnonymousClass1 x1) {
            this();
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

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface BaseEuiccCommandCallback {
        void onEuiccServiceUnavailable();
    }

    private class BindingState extends State {
        private BindingState() {
        }

        /* synthetic */ BindingState(EuiccConnector x0, AnonymousClass1 x1) {
            this();
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

        /* synthetic */ ConnectedState(EuiccConnector x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            EuiccConnector.this.removeMessages(2);
            EuiccConnector.this.sendMessageDelayed(3, 60000);
        }

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
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$1$wTkmDdVlxcrtbVPcCl3t7xD490o(this, callback, eid));
                                }

                                public static /* synthetic */ void lambda$onSuccess$0(AnonymousClass1 anonymousClass1, BaseEuiccCommandCallback callback, String eid) {
                                    ((GetEidCommandCallback) callback).onGetEidComplete(eid);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 101:
                            GetMetadataRequest request = message.obj;
                            EuiccConnector.this.mEuiccService.getDownloadableSubscriptionMetadata(-1, request.mSubscription, request.mForceDeactivateSim, new IGetDownloadableSubscriptionMetadataCallback.Stub() {
                                public void onComplete(GetDownloadableSubscriptionMetadataResult result) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$2$mYGM42yhe76zJekjTAzT10LdEMk(this, callback, result));
                                }

                                public static /* synthetic */ void lambda$onComplete$0(AnonymousClass2 anonymousClass2, BaseEuiccCommandCallback callback, GetDownloadableSubscriptionMetadataResult result) {
                                    ((GetMetadataCommandCallback) callback).onGetMetadataComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 102:
                            DownloadRequest request2 = (DownloadRequest) message.obj;
                            EuiccConnector.this.mEuiccService.downloadSubscription(-1, request2.mSubscription, request2.mSwitchAfterDownload, request2.mForceDeactivateSim, new IDownloadSubscriptionCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$3$kCYyTG6MMZu-1yQLS6p1_Mk7KM8(this, callback, result));
                                }

                                public static /* synthetic */ void lambda$onComplete$0(AnonymousClass3 anonymousClass3, BaseEuiccCommandCallback callback, int result) {
                                    ((DownloadCommandCallback) callback).onDownloadComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case EuiccConnector.CMD_GET_EUICC_PROFILE_INFO_LIST /*103*/:
                            EuiccConnector.this.mEuiccService.getEuiccProfileInfoList(-1, new IGetEuiccProfileInfoListCallback.Stub() {
                                public void onComplete(GetEuiccProfileInfoListResult result) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$4$S52i3hpE3-FGho807KZ1LR5rXQM(this, callback, result));
                                }

                                public static /* synthetic */ void lambda$onComplete$0(AnonymousClass4 anonymousClass4, BaseEuiccCommandCallback callback, GetEuiccProfileInfoListResult result) {
                                    ((GetEuiccProfileInfoListCommandCallback) callback).onListComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 104:
                            EuiccConnector.this.mEuiccService.getDefaultDownloadableSubscriptionList(-1, message.obj.mForceDeactivateSim, new IGetDefaultDownloadableSubscriptionListCallback.Stub() {
                                public void onComplete(GetDefaultDownloadableSubscriptionListResult result) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$5$fNoNRKwweNINlHKYo1LLy2Hd_RA(this, callback, result));
                                }

                                public static /* synthetic */ void lambda$onComplete$0(AnonymousClass5 anonymousClass5, BaseEuiccCommandCallback callback, GetDefaultDownloadableSubscriptionListResult result) {
                                    ((GetDefaultListCommandCallback) callback).onGetDefaultListComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 105:
                            EuiccConnector.this.mEuiccService.getEuiccInfo(-1, new IGetEuiccInfoCallback.Stub() {
                                public void onSuccess(EuiccInfo euiccInfo) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$6$RMNCT6pukGHYhU_7k7HVxbm5IWE(this, callback, euiccInfo));
                                }

                                public static /* synthetic */ void lambda$onSuccess$0(AnonymousClass6 anonymousClass6, BaseEuiccCommandCallback callback, EuiccInfo euiccInfo) {
                                    ((GetEuiccInfoCommandCallback) callback).onGetEuiccInfoComplete(euiccInfo);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 106:
                            EuiccConnector.this.mEuiccService.deleteSubscription(-1, message.obj.mIccid, new IDeleteSubscriptionCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$7$-Ogvr7PIASwQa0kQAqAyfdEKAG4(this, callback, result));
                                }

                                public static /* synthetic */ void lambda$onComplete$0(AnonymousClass7 anonymousClass7, BaseEuiccCommandCallback callback, int result) {
                                    ((DeleteCommandCallback) callback).onDeleteComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case EuiccConnector.CMD_SWITCH_TO_SUBSCRIPTION /*107*/:
                            SwitchRequest request3 = message.obj;
                            EuiccConnector.this.mEuiccService.switchToSubscription(-1, request3.mIccid, request3.mForceDeactivateSim, new ISwitchToSubscriptionCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$8$653ymvVUxXSmc5rF5YXkbNw3yw8(this, callback, result));
                                }

                                public static /* synthetic */ void lambda$onComplete$0(AnonymousClass8 anonymousClass8, BaseEuiccCommandCallback callback, int result) {
                                    ((SwitchCommandCallback) callback).onSwitchComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 108:
                            UpdateNicknameRequest request4 = message.obj;
                            EuiccConnector.this.mEuiccService.updateSubscriptionNickname(-1, request4.mIccid, request4.mNickname, new IUpdateSubscriptionNicknameCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$9$xm26YKGxl72UYoxSNyEMJslmmNk(this, callback, result));
                                }

                                public static /* synthetic */ void lambda$onComplete$0(AnonymousClass9 anonymousClass9, BaseEuiccCommandCallback callback, int result) {
                                    ((UpdateNicknameCommandCallback) callback).onUpdateNicknameComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case EuiccConnector.CMD_ERASE_SUBSCRIPTIONS /*109*/:
                            EuiccConnector.this.mEuiccService.eraseSubscriptions(-1, new IEraseSubscriptionsCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$10$uMqDQsfFYIEEah_N7V76hMlEL94(this, callback, result));
                                }

                                public static /* synthetic */ void lambda$onComplete$0(AnonymousClass10 anonymousClass10, BaseEuiccCommandCallback callback, int result) {
                                    ((EraseCommandCallback) callback).onEraseComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case EuiccConnector.CMD_RETAIN_SUBSCRIPTIONS /*110*/:
                            EuiccConnector.this.mEuiccService.retainSubscriptionsForFactoryReset(-1, new IRetainSubscriptionsForFactoryResetCallback.Stub() {
                                public void onComplete(int result) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$11$yvv0ylXs7V5vymCcYvu3RpgoeDw(this, callback, result));
                                }

                                public static /* synthetic */ void lambda$onComplete$0(AnonymousClass11 anonymousClass11, BaseEuiccCommandCallback callback, int result) {
                                    ((RetainSubscriptionsCommandCallback) callback).onRetainSubscriptionsComplete(result);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 111:
                            EuiccConnector.this.mEuiccService.getOtaStatus(-1, new IGetOtaStatusCallback.Stub() {
                                public void onSuccess(int status) {
                                    EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$12$wYal9P4llN7g9YAk_zACL8m3nS0(this, callback, status));
                                }

                                public static /* synthetic */ void lambda$onSuccess$0(AnonymousClass12 anonymousClass12, BaseEuiccCommandCallback callback, int status) {
                                    ((GetOtaStatusCommandCallback) callback).onGetOtaStatusComplete(status);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        case 112:
                            EuiccConnector.this.mEuiccService.startOtaIfNecessary(-1, new IOtaStatusChangedCallback.Stub() {
                                public void onOtaStatusChanged(int status) throws RemoteException {
                                    if (status == 1) {
                                        EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$13$5nh8TOHvAdIIa_S3V0gwsRICKC4(callback, status));
                                    } else {
                                        EuiccConnector.this.sendMessage(6, new -$$Lambda$EuiccConnector$ConnectedState$13$REfW_lBcrAssQONSKwOlO3PX83k(this, callback, status));
                                    }
                                }

                                public static /* synthetic */ void lambda$onOtaStatusChanged$1(AnonymousClass13 anonymousClass13, BaseEuiccCommandCallback callback, int status) {
                                    ((OtaStatusChangedCallback) callback).onOtaStatusChanged(status);
                                    EuiccConnector.this.onCommandEnd(callback);
                                }
                            });
                            break;
                        default:
                            String str = EuiccConnector.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Unimplemented eUICC command: ");
                            stringBuilder.append(message.what);
                            Log.wtf(str, stringBuilder.toString());
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

    static class DeleteRequest {
        DeleteCommandCallback mCallback;
        String mIccid;

        DeleteRequest() {
        }
    }

    private class DisconnectedState extends State {
        private DisconnectedState() {
        }

        /* synthetic */ DisconnectedState(EuiccConnector x0, AnonymousClass1 x1) {
            this();
        }

        public void enter() {
            EuiccConnector.this.sendMessageDelayed(2, 30000);
        }

        public boolean processMessage(Message message) {
            if (message.what == 4) {
                EuiccConnector.this.mEuiccService = (IEuiccService) message.obj;
                EuiccConnector.this.transitionTo(EuiccConnector.this.mConnectedState);
                return true;
            }
            boolean forceRebind = false;
            if (message.what == 1) {
                ServiceInfo bestComponent = EuiccConnector.this.findBestComponent();
                String affectedPackage = message.obj;
                boolean isSameComponent = bestComponent == null ? EuiccConnector.this.mSelectedComponent != null : EuiccConnector.this.mSelectedComponent == null || Objects.equals(bestComponent.getComponentName(), EuiccConnector.this.mSelectedComponent.getComponentName());
                if (bestComponent != null && Objects.equals(bestComponent.packageName, affectedPackage)) {
                    forceRebind = true;
                }
                if (!isSameComponent || forceRebind) {
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

    static class DownloadRequest {
        DownloadCommandCallback mCallback;
        boolean mForceDeactivateSim;
        DownloadableSubscription mSubscription;
        boolean mSwitchAfterDownload;

        DownloadRequest() {
        }
    }

    private class EuiccPackageMonitor extends PackageMonitor {
        private EuiccPackageMonitor() {
        }

        /* synthetic */ EuiccPackageMonitor(EuiccConnector x0, AnonymousClass1 x1) {
            this();
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
            EuiccConnector.this.sendMessage(1, forceUnbindForThisPackage ? packageName : null);
        }
    }

    static class GetDefaultListRequest {
        GetDefaultListCommandCallback mCallback;
        boolean mForceDeactivateSim;

        GetDefaultListRequest() {
        }
    }

    static class GetMetadataRequest {
        GetMetadataCommandCallback mCallback;
        boolean mForceDeactivateSim;
        DownloadableSubscription mSubscription;

        GetMetadataRequest() {
        }
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

        /* synthetic */ UnavailableState(EuiccConnector x0, AnonymousClass1 x1) {
            this();
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

    static class UpdateNicknameRequest {
        UpdateNicknameCommandCallback mCallback;
        String mIccid;
        String mNickname;

        UpdateNicknameRequest() {
        }
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface DeleteCommandCallback extends BaseEuiccCommandCallback {
        void onDeleteComplete(int i);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface DownloadCommandCallback extends BaseEuiccCommandCallback {
        void onDownloadComplete(int i);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface EraseCommandCallback extends BaseEuiccCommandCallback {
        void onEraseComplete(int i);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface GetDefaultListCommandCallback extends BaseEuiccCommandCallback {
        void onGetDefaultListComplete(GetDefaultDownloadableSubscriptionListResult getDefaultDownloadableSubscriptionListResult);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface GetEidCommandCallback extends BaseEuiccCommandCallback {
        void onGetEidComplete(String str);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface GetEuiccInfoCommandCallback extends BaseEuiccCommandCallback {
        void onGetEuiccInfoComplete(EuiccInfo euiccInfo);
    }

    interface GetEuiccProfileInfoListCommandCallback extends BaseEuiccCommandCallback {
        void onListComplete(GetEuiccProfileInfoListResult getEuiccProfileInfoListResult);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface GetMetadataCommandCallback extends BaseEuiccCommandCallback {
        void onGetMetadataComplete(GetDownloadableSubscriptionMetadataResult getDownloadableSubscriptionMetadataResult);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface GetOtaStatusCommandCallback extends BaseEuiccCommandCallback {
        void onGetOtaStatusComplete(int i);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface OtaStatusChangedCallback extends BaseEuiccCommandCallback {
        void onOtaStatusChanged(int i);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface RetainSubscriptionsCommandCallback extends BaseEuiccCommandCallback {
        void onRetainSubscriptionsComplete(int i);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface SwitchCommandCallback extends BaseEuiccCommandCallback {
        void onSwitchComplete(int i);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public interface UpdateNicknameCommandCallback extends BaseEuiccCommandCallback {
        void onUpdateNicknameComplete(int i);
    }

    private static boolean isEuiccCommand(int what) {
        return what >= 100;
    }

    public static ActivityInfo findBestActivity(PackageManager packageManager, Intent intent) {
        ActivityInfo bestComponent = (ActivityInfo) findBestComponent(packageManager, packageManager.queryIntentActivities(intent, EUICC_QUERY_FLAGS));
        if (bestComponent == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("No valid component found for intent: ");
            stringBuilder.append(intent);
            Log.w(str, stringBuilder.toString());
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

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public EuiccConnector(Context context, Looper looper) {
        super(TAG, looper);
        init(context);
    }

    private void init(Context context) {
        this.mContext = context;
        this.mPm = context.getPackageManager();
        this.mUnavailableState = new UnavailableState(this, null);
        addState(this.mUnavailableState);
        this.mAvailableState = new AvailableState(this, null);
        addState(this.mAvailableState, this.mUnavailableState);
        this.mBindingState = new BindingState(this, null);
        addState(this.mBindingState);
        this.mDisconnectedState = new DisconnectedState(this, null);
        addState(this.mDisconnectedState);
        this.mConnectedState = new ConnectedState(this, null);
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

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void getEid(GetEidCommandCallback callback) {
        sendMessage(100, callback);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void getOtaStatus(GetOtaStatusCommandCallback callback) {
        sendMessage(111, callback);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void startOtaIfNecessary(OtaStatusChangedCallback callback) {
        sendMessage(112, callback);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void getDownloadableSubscriptionMetadata(DownloadableSubscription subscription, boolean forceDeactivateSim, GetMetadataCommandCallback callback) {
        GetMetadataRequest request = new GetMetadataRequest();
        request.mSubscription = subscription;
        request.mForceDeactivateSim = forceDeactivateSim;
        request.mCallback = callback;
        sendMessage(101, request);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
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

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void getDefaultDownloadableSubscriptionList(boolean forceDeactivateSim, GetDefaultListCommandCallback callback) {
        GetDefaultListRequest request = new GetDefaultListRequest();
        request.mForceDeactivateSim = forceDeactivateSim;
        request.mCallback = callback;
        sendMessage(104, request);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void getEuiccInfo(GetEuiccInfoCommandCallback callback) {
        sendMessage(105, callback);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void deleteSubscription(String iccid, DeleteCommandCallback callback) {
        DeleteRequest request = new DeleteRequest();
        request.mIccid = iccid;
        request.mCallback = callback;
        sendMessage(106, request);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void switchToSubscription(String iccid, boolean forceDeactivateSim, SwitchCommandCallback callback) {
        SwitchRequest request = new SwitchRequest();
        request.mIccid = iccid;
        request.mForceDeactivateSim = forceDeactivateSim;
        request.mCallback = callback;
        sendMessage(CMD_SWITCH_TO_SUBSCRIPTION, request);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void updateSubscriptionNickname(String iccid, String nickname, UpdateNicknameCommandCallback callback) {
        UpdateNicknameRequest request = new UpdateNicknameRequest();
        request.mIccid = iccid;
        request.mNickname = nickname;
        request.mCallback = callback;
        sendMessage(108, request);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
    public void eraseSubscriptions(EraseCommandCallback callback) {
        sendMessage(CMD_ERASE_SUBSCRIPTIONS, callback);
    }

    @VisibleForTesting(visibility = Visibility.PACKAGE)
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
            case 111:
            case 112:
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
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unsupported message: ");
                stringBuilder.append(message.what);
                throw new IllegalArgumentException(stringBuilder.toString());
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
                if (isValidEuiccComponent(packageManager, resolveInfo)) {
                    if (resolveInfo.filter.getPriority() > bestPriority) {
                        bestPriority = resolveInfo.filter.getPriority();
                        bestComponent = resolveInfo.getComponentInfo();
                    }
                }
            }
        }
        return bestComponent;
    }

    private static boolean isValidEuiccComponent(PackageManager packageManager, ResolveInfo resolveInfo) {
        ComponentInfo componentInfo = resolveInfo.getComponentInfo();
        String packageName = componentInfo.getComponentName().getPackageName();
        String str;
        if (packageManager.checkPermission("android.permission.WRITE_EMBEDDED_SUBSCRIPTIONS", packageName) != 0) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Package ");
            stringBuilder.append(packageName);
            stringBuilder.append(" does not declare WRITE_EMBEDDED_SUBSCRIPTIONS");
            Log.wtf(str, stringBuilder.toString());
            return false;
        }
        if (componentInfo instanceof ServiceInfo) {
            str = ((ServiceInfo) componentInfo).permission;
        } else if (componentInfo instanceof ActivityInfo) {
            str = ((ActivityInfo) componentInfo).permission;
        } else {
            throw new IllegalArgumentException("Can only verify services/activities");
        }
        String str2;
        StringBuilder stringBuilder2;
        if (!TextUtils.equals(str, "android.permission.BIND_EUICC_SERVICE")) {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Package ");
            stringBuilder2.append(packageName);
            stringBuilder2.append(" does not require the BIND_EUICC_SERVICE permission");
            Log.wtf(str2, stringBuilder2.toString());
            return false;
        } else if (resolveInfo.filter != null && resolveInfo.filter.getPriority() != 0) {
            return true;
        } else {
            str2 = TAG;
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Package ");
            stringBuilder2.append(packageName);
            stringBuilder2.append(" does not specify a priority");
            Log.wtf(str2, stringBuilder2.toString());
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
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unhandled message ");
        stringBuilder.append(msg.what);
        stringBuilder.append(" in state ");
        stringBuilder.append(state == null ? "null" : state.getName());
        Log.wtf(str, stringBuilder.toString());
    }

    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("mSelectedComponent=");
        stringBuilder.append(this.mSelectedComponent);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mEuiccService=");
        stringBuilder.append(this.mEuiccService);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("mActiveCommandCount=");
        stringBuilder.append(this.mActiveCommandCallbacks.size());
        pw.println(stringBuilder.toString());
    }
}
