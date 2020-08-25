package com.android.server.input;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.IHwBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.server.LocalServices;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.policy.WindowManagerPolicy;
import com.android.server.rms.iaware.appmng.AwareFakeActivityRecg;
import com.android.server.rms.iaware.cpu.CPUCustBaseConfig;
import com.huawei.android.hardware.input.IHwTHPEventListener;
import com.huawei.sidetouch.HwSideTouchManager;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import vendor.huawei.hardware.tp.V1_0.ITPCallback;
import vendor.huawei.hardware.tp.V1_0.ITouchscreen;

public final class HwInputManagerServiceEx implements IHwInputManagerServiceEx {
    public static final String[] CALLING_PACKAGES = {"com.baidu.input_huawei"};
    private static final String CMD_EXE_SUCCESS = "OK";
    private static final String CMD_INFORM_APP_CRASH = "THP_InformUserAppCrash";
    private static final String HAS_SHOW_DIALOG = "has_show_dialog";
    protected static final boolean IS_DEBUG_ON = Log.HWINFO;
    private static final String IS_TABLET = SystemProperties.get("ro.build.characteristics", "");
    private static final int MAIN_PID = 1000;
    private static final String PACKAGE_NAME_SYSTEMUI = "android.uid.systemui";
    private static final String TAG = "HwInputManagerServiceEx";
    private static final int THP_HAL_DEATH_COOKIE = 1000;
    private AlertDialog mAlterSoftInputDialog = null;
    /* access modifiers changed from: private */
    public final Callbacks mCallbacks;
    /* access modifiers changed from: private */
    public final Map<IBinder, ClientState> mClients = new ArrayMap();
    /* access modifiers changed from: private */
    public final Context mContext;
    private IHwInputManagerInner mImsInner = null;
    private boolean mIsShow = false;
    /* access modifiers changed from: private */
    public final Object mLock = new Object();
    private String mResult = null;
    private final ServiceNotification mServiceNotification = new ServiceNotification();
    private HalCallback mThpCallback;
    /* access modifiers changed from: private */
    public ITouchscreen mTpHal = null;

    public HwInputManagerServiceEx(IHwInputManagerInner ims, Context context) {
        this.mImsInner = ims;
        this.mContext = context;
        this.mThpCallback = new HalCallback();
        this.mCallbacks = new Callbacks(BackgroundThread.getHandler().getLooper());
        try {
            IServiceManager serviceManager = IServiceManager.getService();
            if (serviceManager == null || !serviceManager.registerForNotifications(ITouchscreen.kInterfaceName, "", this.mServiceNotification)) {
                Slog.e(TAG, "Failed to get serviceManager and register service start notification");
            }
            connectToHidl();
        } catch (RemoteException e) {
            Slog.e(TAG, "Failed to register service start notification", e);
        }
    }

    /* access modifiers changed from: private */
    /* JADX WARNING: Code restructure failed: missing block: B:15:0x002a, code lost:
        android.util.Slog.i(com.android.server.input.HwInputManagerServiceEx.TAG, "Successfully connect to TP service!");
        r1 = r5.mClients;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:16:0x0033, code lost:
        monitor-enter(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:19:0x003b, code lost:
        if (r5.mClients.size() < 1) goto L_0x0042;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:20:0x003d, code lost:
        setThpCallback(r5.mThpCallback);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:21:0x0042, code lost:
        monitor-exit(r1);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:22:0x0043, code lost:
        return;
     */
    public void connectToHidl() {
        synchronized (this.mLock) {
            if (this.mTpHal == null) {
                try {
                    this.mTpHal = ITouchscreen.getService();
                    if (this.mTpHal == null) {
                        Slog.e(TAG, "Failed to get ITouchscreen service");
                        return;
                    }
                    this.mTpHal.linkToDeath(new DeathRecipient(), 1000);
                } catch (RemoteException e) {
                    Slog.e(TAG, "Failed to register service start notification Exception " + e);
                } catch (NoSuchElementException e2) {
                    Slog.e(TAG, "Failed to register service start notification NoSuchElementException");
                }
            }
        }
    }

    private ClientState getClient(IBinder token) {
        ClientState client;
        synchronized (this.mClients) {
            client = this.mClients.get(token);
            if (client == null) {
                client = new ClientState(token);
                this.mClients.put(token, client);
            }
        }
        return client;
    }

    private void enforceCallingPermission(String packageName, int uid) {
        if (uid != 1000) {
            if (packageName == null || !packageName.contains(PACKAGE_NAME_SYSTEMUI)) {
                String[] strArr = CALLING_PACKAGES;
                int length = strArr.length;
                int i = 0;
                while (true) {
                    if (i >= length) {
                        break;
                    } else if (strArr[i].equals(packageName)) {
                        try {
                            ApplicationInfo appInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, 0);
                            if (appInfo != null && (appInfo.flags & 1) != 0) {
                                return;
                            }
                        } catch (PackageManager.NameNotFoundException e) {
                            Slog.e(TAG, packageName + " not found.");
                        }
                    } else {
                        i++;
                    }
                }
                throw new SecurityException("Package : " + packageName + ",uid : " + uid + " does not have permission to access THP interfaces");
            }
        }
    }

    private String runHwThpCommandInternal(String command, String parameter) {
        String str;
        synchronized (this.mLock) {
            this.mResult = null;
            try {
                if (this.mTpHal != null) {
                    this.mTpHal.hwTsRunCommand(command, parameter, new ITouchscreen.hwTsRunCommandCallback(command, parameter) {
                        /* class com.android.server.input.$$Lambda$HwInputManagerServiceEx$AdQnz5IPfrPDx5_Vu5ZKPIjwyT8 */
                        private final /* synthetic */ String f$1;
                        private final /* synthetic */ String f$2;

                        {
                            this.f$1 = r2;
                            this.f$2 = r3;
                        }

                        @Override // vendor.huawei.hardware.tp.V1_0.ITouchscreen.hwTsRunCommandCallback
                        public final void onValues(int i, String str) {
                            HwInputManagerServiceEx.this.lambda$runHwThpCommandInternal$0$HwInputManagerServiceEx(this.f$1, this.f$2, i, str);
                        }
                    });
                } else if (IS_DEBUG_ON) {
                    Slog.i(TAG, "runHwTHPCommand failed, TP service has not been initialized yet !!");
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "hwTsGetCapacityInfo Exception");
            }
            str = this.mResult;
        }
        return str;
    }

    public /* synthetic */ void lambda$runHwThpCommandInternal$0$HwInputManagerServiceEx(String command, String parameter, int ret, String status) {
        if (IS_DEBUG_ON) {
            Slog.i(TAG, "runHwTHPCommand command : " + command + ",parameter : " + parameter + ", ret = " + ret + ", status = " + status);
        }
        this.mResult = status;
    }

    public String runHwTHPCommand(String command, String parameter) {
        int uid = Binder.getCallingUid();
        String pacakgeName = this.mContext.getPackageManager().getNameForUid(uid);
        enforceCallingPermission(pacakgeName, uid);
        return runHwThpCommandInternal(command, pacakgeName + CPUCustBaseConfig.CPUCONFIG_INVALID_STR + parameter);
    }

    public void registerListener(IHwTHPEventListener listener, IBinder binder) {
        int uid = Binder.getCallingUid();
        String pkgName = this.mContext.getPackageManager().getNameForUid(uid);
        this.mCallbacks.register(listener);
        synchronized (this.mClients) {
            getClient(binder).addListenerLocked(listener);
            if (this.mClients.size() == 1) {
                setThpCallback(this.mThpCallback);
            }
        }
        if (IS_DEBUG_ON) {
            Slog.i(TAG, "registerListener listener = " + listener.asBinder() + ",uid = " + uid + ",calling pkgName = " + pkgName);
        }
    }

    public void unregisterListener(IHwTHPEventListener listener, IBinder binder) {
        int uid = Binder.getCallingUid();
        String pkgName = this.mContext.getPackageManager().getNameForUid(uid);
        this.mCallbacks.unregister(listener);
        synchronized (this.mClients) {
            getClient(binder).removeListenerLocked(listener);
            if (this.mClients.size() == 0) {
                setThpCallback(null);
            }
        }
        if (IS_DEBUG_ON) {
            Slog.i(TAG, "unregisterListener listener = " + listener.asBinder() + ",uid = " + uid + ",calling pkgName = " + pkgName);
        }
    }

    public void checkHasShowDismissSoftInputAlertDialog(boolean isEmpty) {
        if ("tablet".equals(IS_TABLET) && !isMmiTesting() && !isEmpty && !this.mIsShow) {
            if (!(HwPCUtils.enabledInPad() && HwPCUtils.isPcCastMode())) {
                int showIme = Settings.Secure.getInt(this.mContext.getContentResolver(), "show_ime_with_hard_keyboard", 0);
                int showDialog = Settings.Secure.getInt(this.mContext.getContentResolver(), HAS_SHOW_DIALOG, 0);
                Log.i(TAG, "checkHasShowDismissSoftInputAlertDialog - showIme =" + showIme + "showDialog =" + showDialog);
                if (showIme == 0 && showDialog == 0) {
                    showDismissSoftInputAlertDialog(this.mContext);
                    Settings.Secure.putInt(this.mContext.getContentResolver(), HAS_SHOW_DIALOG, 1);
                }
                this.mIsShow = true;
            }
        }
    }

    private boolean isMmiTesting() {
        return "true".equals(SystemProperties.get("runtime.mmitest.isrunning", "false"));
    }

    private void showDismissSoftInputAlertDialog(Context context) {
        Log.i(TAG, "showDismissSoftInputAlertDialog");
        AlertDialog alertDialog = this.mAlterSoftInputDialog;
        if (alertDialog == null || !alertDialog.isShowing()) {
            AlertDialog.Builder buider = new AlertDialog.Builder(context, 33947691);
            View view = LayoutInflater.from(buider.getContext()).inflate(34013419, (ViewGroup) null);
            if (view != null) {
                ImageView imageView = (ImageView) view.findViewById(34603405);
                TextView textView = (TextView) view.findViewById(34603403);
                if (imageView != null && textView != null) {
                    imageView.setImageResource(33752007);
                    textView.setText(context.getResources().getString(33686073));
                    this.mAlterSoftInputDialog = buider.setTitle(33686075).setPositiveButton(33686074, $$Lambda$HwInputManagerServiceEx$3Xt6EEjK_GXDs03sW6_dm0X4uo.INSTANCE).setView(view).create();
                    this.mAlterSoftInputDialog.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_BIND_FAIL);
                    this.mAlterSoftInputDialog.show();
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void setThpCallback(HalCallback callback) {
        synchronized (this.mLock) {
            try {
                if (this.mTpHal != null) {
                    if (IS_DEBUG_ON) {
                        Slog.i(TAG, "setThpCallback hwTsSetCallback callback = " + callback);
                    }
                    this.mTpHal.hwTsSetCallback(callback);
                } else if (IS_DEBUG_ON) {
                    Slog.i(TAG, "setThpCallback failed, TP service has not been initialized yet !!");
                }
            } catch (RemoteException e) {
                Slog.e(TAG, "hwTsSetCallback Exception e");
            }
        }
    }

    public void notifyNativeEvent(int eventType, int eventValue, int keyAction, int pid, int uid) {
        AwareFakeActivityRecg.self().processNativeEventNotify(eventType, eventValue, keyAction, pid, uid);
    }

    private boolean checkSystemApp(String pkg) {
        Context context;
        if (pkg == null || (context = this.mContext) == null) {
            Log.e(TAG, "pkg or mContext is null");
            return false;
        }
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            Log.e(TAG, "get packageManager is null");
            return false;
        }
        try {
            ApplicationInfo info = pm.getApplicationInfo(pkg, 0);
            if (info == null) {
                Log.e(TAG, "info is null");
                return false;
            }
            boolean isSystemApp = (info.flags & 1) > 0;
            boolean isUpdatedSystemApp = (info.flags & 128) > 0;
            if (isSystemApp || isUpdatedSystemApp) {
                return true;
            }
            return false;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "get infor error");
            return false;
        }
    }

    public String runSideTouchCommand(String command, String parameter) {
        boolean isPermissioned = this.mContext.checkCallingOrSelfPermission("com.huawei.permission.EXT_DISPLAY_UI_PERMISSION") == 0;
        String packageName = this.mContext.getPackageManager().getNameForUid(Binder.getCallingUid());
        boolean isSystemApp = false;
        if (!isPermissioned) {
            isSystemApp = checkSystemApp(packageName);
        }
        if (isSystemApp || isPermissioned) {
            StringBuilder sb = new StringBuilder(packageName);
            sb.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
            if (parameter != null) {
                sb.append(parameter);
            }
            return runHwThpCommandInternal(command, sb.toString());
        }
        Slog.e(TAG, "package " + packageName + " not permissioned or not system app : " + isSystemApp);
        return null;
    }

    public int[] setTPCommand(int type, Bundle bundle) {
        if (type == 1) {
            notifyVolumePanelStatus(bundle);
        }
        return HwSideTouchManager.getInstance(this.mContext).runSideTouchCommand(type, bundle);
    }

    private void notifyVolumePanelStatus(Bundle bundle) {
        if (bundle != null) {
            boolean isVolumePanelVisible = false;
            int guiState = bundle.getInt("guiState", 0);
            WindowManagerPolicy policy = (WindowManagerPolicy) LocalServices.getService(WindowManagerPolicy.class);
            if (policy != null) {
                if (guiState == 1) {
                    isVolumePanelVisible = true;
                }
                policy.notifyVolumePanelStatus(isVolumePanelVisible);
            }
        }
    }

    /* access modifiers changed from: private */
    public static class Callbacks extends Handler {
        private static final int MSG_THP_INPUT_EVENT = 1;
        private static final int MSG_TP_INPUT_EVENT = 2;
        private final RemoteCallbackList<IHwTHPEventListener> mCallbacks = new RemoteCallbackList<>();

        Callbacks(Looper looper) {
            super(looper);
        }

        public void register(IHwTHPEventListener callback) {
            this.mCallbacks.register(callback);
        }

        public void unregister(IHwTHPEventListener callback) {
            this.mCallbacks.unregister(callback);
        }

        public void handleMessage(Message msg) {
            SomeArgs args = (SomeArgs) msg.obj;
            int broadCasts = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < broadCasts; i++) {
                try {
                    invokeCallback(this.mCallbacks.getBroadcastItem(i), msg.what, args);
                } catch (RemoteException e) {
                    Slog.i(HwInputManagerServiceEx.TAG, "invoke fail");
                }
            }
            this.mCallbacks.finishBroadcast();
            args.recycle();
        }

        private void invokeCallback(IHwTHPEventListener callback, int what, SomeArgs args) throws RemoteException {
            if (what == 1) {
                callback.onHwTHPEvent(((Integer) args.arg1).intValue());
            } else if (what == 2) {
                callback.onHwTpEvent(((Integer) args.arg1).intValue(), ((Integer) args.arg2).intValue(), (String) args.arg3);
            }
        }

        /* access modifiers changed from: private */
        public void onHwThpEvent(int event) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Integer.valueOf(event);
            obtainMessage(1, args).sendToTarget();
        }

        /* access modifiers changed from: private */
        public void onHwTpEvent(int eventClass, int eventCode, String extraInfo) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Integer.valueOf(eventClass);
            args.arg2 = Integer.valueOf(eventCode);
            args.arg3 = extraInfo;
            obtainMessage(2, args).sendToTarget();
        }
    }

    final class ServiceNotification extends IServiceNotification.Stub {
        ServiceNotification() {
        }

        public void onRegistration(String fqName, String name, boolean isPreexisting) {
            HwInputManagerServiceEx.this.connectToHidl();
        }
    }

    public final class ClientState extends Binder implements IBinder.DeathRecipient {
        private final IBinder mAppToken;
        private final Map<IBinder, IHwTHPEventListener> mListeners = new HashMap();
        private final int mPid = Binder.getCallingPid();
        private final int mUid = Binder.getCallingUid();

        public ClientState(IBinder appToken) {
            this.mAppToken = appToken;
            try {
                this.mAppToken.linkToDeath(this, 0);
            } catch (RemoteException e) {
                Slog.i(HwInputManagerServiceEx.TAG, "app token exception.");
            }
        }

        public void addListenerLocked(IHwTHPEventListener listener) {
            this.mListeners.put(listener.asBinder(), listener);
        }

        public void removeListenerLocked(IHwTHPEventListener listener) {
            this.mListeners.remove(listener.asBinder());
            if (this.mListeners.size() == 0) {
                closeClientsLocked();
            }
        }

        private void closeClientsLocked() {
            HwInputManagerServiceEx.this.mClients.remove(this.mAppToken);
            this.mAppToken.unlinkToDeath(this, 0);
        }

        public void binderDied() {
            synchronized (HwInputManagerServiceEx.this.mClients) {
                Slog.d(HwInputManagerServiceEx.TAG, "Client died: " + this);
                HwInputManagerServiceEx.this.mClients.remove(this.mAppToken);
                if (HwInputManagerServiceEx.this.mClients.size() == 0) {
                    HwInputManagerServiceEx.this.setThpCallback(null);
                }
            }
            String result = HwInputManagerServiceEx.this.runHwTHPCommand(HwInputManagerServiceEx.CMD_INFORM_APP_CRASH, "");
            if (HwInputManagerServiceEx.CMD_EXE_SUCCESS.equals(result)) {
                Slog.d(HwInputManagerServiceEx.TAG, "CMD_INFORM_APP_CRASH exec failed, result = " + result);
            }
        }

        public String toString() {
            return "Client: UID: " + this.mUid + " PID: " + this.mPid + " listener count: " + this.mListeners.size();
        }
    }

    /* access modifiers changed from: private */
    public class HalCallback extends ITPCallback.Stub {
        private HalCallback() {
        }

        @Override // vendor.huawei.hardware.tp.V1_0.ITPCallback
        public void notifyTHPEvents(int event, int retval) {
            HwSideTouchManager sideTouchManager;
            if (HwInputManagerServiceEx.IS_DEBUG_ON) {
                Slog.i(HwInputManagerServiceEx.TAG, "Receive a THP event from TP driver event = " + event + ",retval = " + retval);
            }
            if (!(HwInputManagerServiceEx.this.mContext == null || (sideTouchManager = HwSideTouchManager.getInstance(HwInputManagerServiceEx.this.mContext)) == null)) {
                sideTouchManager.notifySideTouchManager(event);
            }
            HwInputManagerServiceEx.this.mCallbacks.onHwThpEvent(event);
        }

        @Override // vendor.huawei.hardware.tp.V1_0.ITPCallback
        public void notifyTPEvents(int eventClass, int eventCode, String extraInfo) {
            Log.i(HwInputManagerServiceEx.TAG, "Receive a TP driver eventClass=" + eventClass + ", eventCode=" + eventCode + ",extraInfo =" + extraInfo);
            HwInputManagerServiceEx.this.mCallbacks.onHwTpEvent(eventClass, eventCode, extraInfo);
        }
    }

    final class DeathRecipient implements IHwBinder.DeathRecipient {
        DeathRecipient() {
        }

        public void serviceDied(long cookie) {
            Slog.w(HwInputManagerServiceEx.TAG, "TP service has died");
            if (cookie == 1000) {
                synchronized (HwInputManagerServiceEx.this.mLock) {
                    ITouchscreen unused = HwInputManagerServiceEx.this.mTpHal = null;
                }
            }
        }
    }
}
