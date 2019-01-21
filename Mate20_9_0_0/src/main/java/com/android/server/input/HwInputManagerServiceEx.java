package com.android.server.input;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.hidl.manager.V1_0.IServiceManager;
import android.hidl.manager.V1_0.IServiceNotification.Stub;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.util.ArrayMap;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.Slog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import com.android.internal.os.BackgroundThread;
import com.android.internal.os.SomeArgs;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.huawei.android.hardware.input.IHwTHPEventListener;
import java.util.HashMap;
import vendor.huawei.hardware.tp.V1_0.ITPCallback;
import vendor.huawei.hardware.tp.V1_0.ITouchscreen;

public final class HwInputManagerServiceEx implements IHwInputManagerServiceEx {
    public static final String[] CALLING_PACKAGES = new String[]{"com.baidu.input_huawei"};
    private static final String CMD_EXE_SUCCESS = "OK";
    private static final String CMD_INFORM_APP_CRASH = "THP_InformUserAppCrash";
    protected static final boolean DEBUG = Log.HWINFO;
    private static final String HAS_SHOW_DIALOG = "has_show_dialog";
    private static final String IS_TABLET = SystemProperties.get("ro.build.characteristics", "");
    private static final String TAG = "HwInputManagerServiceEx";
    private static final int THP_HAL_DEATH_COOKIE = 1000;
    private AlertDialog mAlterSoftInputDialog = null;
    private final Callbacks mCallbacks;
    final ArrayMap<IBinder, ClientState> mClients = new ArrayMap();
    private final Context mContext;
    private boolean mHasShow = false;
    private IHwInputManagerInner mImsInner = null;
    private final Object mLock = new Object();
    private String mResult = null;
    private final ServiceNotification mServiceNotification = new ServiceNotification();
    private HALCallback mTHPCallback;
    private ITouchscreen mTPHal = null;

    private static class Callbacks extends Handler {
        private static final int MSG_THP_INPUT_EVENT = 1;
        private final RemoteCallbackList<IHwTHPEventListener> mCallbacks = new RemoteCallbackList();

        public Callbacks(Looper looper) {
            super(looper);
        }

        public void register(IHwTHPEventListener callback) {
            this.mCallbacks.register(callback);
        }

        public void unregister(IHwTHPEventListener callback) {
            this.mCallbacks.unregister(callback);
        }

        public void handleMessage(Message msg) {
            SomeArgs args = msg.obj;
            int n = this.mCallbacks.beginBroadcast();
            for (int i = 0; i < n; i++) {
                try {
                    invokeCallback((IHwTHPEventListener) this.mCallbacks.getBroadcastItem(i), msg.what, args);
                } catch (RemoteException e) {
                }
            }
            this.mCallbacks.finishBroadcast();
            args.recycle();
        }

        private void invokeCallback(IHwTHPEventListener callback, int what, SomeArgs args) throws RemoteException {
            if (what == 1) {
                callback.onHwTHPEvent(((Integer) args.arg1).intValue());
            }
        }

        private void onHwTHPEvent(int event) {
            SomeArgs args = SomeArgs.obtain();
            args.arg1 = Integer.valueOf(event);
            obtainMessage(1, args).sendToTarget();
        }
    }

    public final class ClientState extends Binder implements android.os.IBinder.DeathRecipient {
        private final IBinder mAppToken;
        private final HashMap<IBinder, IHwTHPEventListener> mListeners = new HashMap();
        private final int mPid = Binder.getCallingPid();
        private final int mUid = Binder.getCallingUid();

        public ClientState(IBinder appToken) {
            this.mAppToken = appToken;
            try {
                this.mAppToken.linkToDeath(this, 0);
            } catch (RemoteException e) {
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
            String str;
            StringBuilder stringBuilder;
            synchronized (HwInputManagerServiceEx.this.mClients) {
                str = HwInputManagerServiceEx.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Client died: ");
                stringBuilder.append(this);
                Slog.d(str, stringBuilder.toString());
                HwInputManagerServiceEx.this.mClients.remove(this.mAppToken);
                if (HwInputManagerServiceEx.this.mClients.size() == 0) {
                    HwInputManagerServiceEx.this.setTHPCallback(null);
                }
            }
            String result = HwInputManagerServiceEx.this.runHwTHPCommand(HwInputManagerServiceEx.CMD_INFORM_APP_CRASH, "");
            if (HwInputManagerServiceEx.CMD_EXE_SUCCESS.equals(result)) {
                str = HwInputManagerServiceEx.TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("CMD_INFORM_APP_CRASH exec failed, result = ");
                stringBuilder.append(result);
                Slog.d(str, stringBuilder.toString());
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("Client: UID: ");
            sb.append(this.mUid);
            sb.append(" PID: ");
            sb.append(this.mPid);
            sb.append(" listener count: ");
            sb.append(this.mListeners.size());
            return sb.toString();
        }
    }

    final class ServiceNotification extends Stub {
        ServiceNotification() {
        }

        public void onRegistration(String fqName, String name, boolean preexisting) {
            HwInputManagerServiceEx.this.connectToHidl();
        }
    }

    final class DeathRecipient implements android.os.IHwBinder.DeathRecipient {
        DeathRecipient() {
        }

        public void serviceDied(long cookie) {
            Slog.w(HwInputManagerServiceEx.TAG, "TP service has died");
            if (cookie == 1000) {
                synchronized (HwInputManagerServiceEx.this.mLock) {
                    HwInputManagerServiceEx.this.mTPHal = null;
                }
            }
        }
    }

    private class HALCallback extends ITPCallback.Stub {
        private HALCallback() {
        }

        public void notifyTHPEvents(int event, int retval) {
            if (HwInputManagerServiceEx.DEBUG) {
                String str = HwInputManagerServiceEx.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Receive a THP event from TP driver event = ");
                stringBuilder.append(event);
                stringBuilder.append(",retval = ");
                stringBuilder.append(retval);
                Slog.i(str, stringBuilder.toString());
            }
            HwInputManagerServiceEx.this.mCallbacks.onHwTHPEvent(event);
        }
    }

    public HwInputManagerServiceEx(IHwInputManagerInner ims, Context context) {
        this.mImsInner = ims;
        this.mContext = context;
        this.mTHPCallback = new HALCallback();
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

    /* JADX WARNING: Missing block: B:18:0x002a, code skipped:
            android.util.Slog.i(TAG, "Successfully connect to TP service!");
            r1 = r5.mClients;
     */
    /* JADX WARNING: Missing block: B:19:0x0033, code skipped:
            monitor-enter(r1);
     */
    /* JADX WARNING: Missing block: B:22:0x003b, code skipped:
            if (r5.mClients.size() < 1) goto L_0x0042;
     */
    /* JADX WARNING: Missing block: B:23:0x003d, code skipped:
            setTHPCallback(r5.mTHPCallback);
     */
    /* JADX WARNING: Missing block: B:24:0x0042, code skipped:
            monitor-exit(r1);
     */
    /* JADX WARNING: Missing block: B:25:0x0043, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void connectToHidl() {
        synchronized (this.mLock) {
            if (this.mTPHal != null) {
                return;
            }
            try {
                this.mTPHal = ITouchscreen.getService();
                if (this.mTPHal == null) {
                    Slog.e(TAG, "Failed to get ITouchscreen service");
                    return;
                }
                this.mTPHal.linkToDeath(new DeathRecipient(), 1000);
            } catch (RemoteException e) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Failed to register service start notification Exception ");
                stringBuilder.append(e);
                Slog.e(str, stringBuilder.toString());
            }
        }
    }

    private ClientState getClient(IBinder token) {
        ClientState client;
        synchronized (this.mClients) {
            client = (ClientState) this.mClients.get(token);
            if (client == null) {
                client = new ClientState(token);
                this.mClients.put(token, client);
            }
        }
        return client;
    }

    private void enforceCallingPermission(String packageName, int uid) {
        if (uid != 1000) {
            StringBuilder stringBuilder;
            for (String equals : CALLING_PACKAGES) {
                if (equals.equals(packageName)) {
                    try {
                        ApplicationInfo appInfo = this.mContext.getPackageManager().getApplicationInfo(packageName, 0);
                        if (!(appInfo == null || (appInfo.flags & 1) == 0)) {
                            return;
                        }
                    } catch (NameNotFoundException e) {
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(packageName);
                        stringBuilder2.append(" not found.");
                        Slog.e(TAG, stringBuilder2.toString());
                    }
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Package : ");
                    stringBuilder.append(packageName);
                    stringBuilder.append(",uid : ");
                    stringBuilder.append(uid);
                    stringBuilder.append(" does not have permission to access THP interfaces");
                    throw new SecurityException(stringBuilder.toString());
                }
            }
            stringBuilder = new StringBuilder();
            stringBuilder.append("Package : ");
            stringBuilder.append(packageName);
            stringBuilder.append(",uid : ");
            stringBuilder.append(uid);
            stringBuilder.append(" does not have permission to access THP interfaces");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private String runHwTHPCommandInternal(String command, String parameter) {
        String str;
        synchronized (this.mLock) {
            this.mResult = null;
            try {
                if (this.mTPHal != null) {
                    this.mTPHal.hwTsRunCommand(command, parameter, new -$$Lambda$HwInputManagerServiceEx$yMXuC-Dz7KO20kGF8SWfRY9towg(this, command, parameter));
                } else if (DEBUG) {
                    Slog.i(TAG, "runHwTHPCommand failed, TP service has not been initialized yet !!");
                }
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("hwTsGetCapacityInfo Exception e = ");
                stringBuilder.append(e);
                Slog.e(str2, stringBuilder.toString());
            }
            str = this.mResult;
        }
        return str;
    }

    public static /* synthetic */ void lambda$runHwTHPCommandInternal$0(HwInputManagerServiceEx hwInputManagerServiceEx, String command, String parameter, int ret, String status) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("runHwTHPCommand command : ");
            stringBuilder.append(command);
            stringBuilder.append(",parameter : ");
            stringBuilder.append(parameter);
            stringBuilder.append(", ret = ");
            stringBuilder.append(ret);
            stringBuilder.append(", status = ");
            stringBuilder.append(status);
            Slog.i(str, stringBuilder.toString());
        }
        hwInputManagerServiceEx.mResult = status;
    }

    public String runHwTHPCommand(String command, String parameter) {
        int uid = Binder.getCallingUid();
        String pacakgeName = this.mContext.getPackageManager().getNameForUid(uid);
        enforceCallingPermission(pacakgeName, uid);
        StringBuilder sb = new StringBuilder(pacakgeName);
        sb.append(CPUCustBaseConfig.CPUCONFIG_INVALID_STR);
        sb.append(parameter);
        return runHwTHPCommandInternal(command, sb.toString());
    }

    public void registerListener(IHwTHPEventListener listener, IBinder iBinder) {
        int uid = Binder.getCallingUid();
        String pkgName = this.mContext.getPackageManager().getNameForUid(uid);
        enforceCallingPermission(pkgName, uid);
        this.mCallbacks.register(listener);
        synchronized (this.mClients) {
            getClient(iBinder).addListenerLocked(listener);
            if (this.mClients.size() == 1) {
                setTHPCallback(this.mTHPCallback);
            }
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("registerListener  listener = ");
            stringBuilder.append(listener.asBinder());
            stringBuilder.append(",uid = ");
            stringBuilder.append(uid);
            stringBuilder.append(",calling pkgName = ");
            stringBuilder.append(pkgName);
            Slog.i(str, stringBuilder.toString());
        }
    }

    public void unregisterListener(IHwTHPEventListener listener, IBinder iBinder) {
        int uid = Binder.getCallingUid();
        String pkgName = this.mContext.getPackageManager().getNameForUid(uid);
        enforceCallingPermission(pkgName, uid);
        this.mCallbacks.unregister(listener);
        synchronized (this.mClients) {
            getClient(iBinder).removeListenerLocked(listener);
            if (this.mClients.size() == 0) {
                setTHPCallback(null);
            }
        }
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("unregisterListener  listener = ");
            stringBuilder.append(listener.asBinder());
            stringBuilder.append(",uid = ");
            stringBuilder.append(uid);
            stringBuilder.append(",calling pkgName = ");
            stringBuilder.append(pkgName);
            Slog.i(str, stringBuilder.toString());
        }
    }

    public void checkHasShowDismissSoftInputAlertDialog(boolean isEmpty) {
        if ("tablet".equals(IS_TABLET) && !isEmpty && !this.mHasShow) {
            boolean isInPcMode = HwPCUtils.enabledInPad() && HwPCUtils.isPcCastMode();
            if (!isInPcMode) {
                int showIme = Secure.getInt(this.mContext.getContentResolver(), "show_ime_with_hard_keyboard", 0);
                int showDialog = Secure.getInt(this.mContext.getContentResolver(), HAS_SHOW_DIALOG, 0);
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("checkHasShowDismissSoftInputAlertDialog - showIme =");
                stringBuilder.append(showIme);
                stringBuilder.append("showDialog =");
                stringBuilder.append(showDialog);
                Log.i(str, stringBuilder.toString());
                if (showIme == 0 && showDialog == 0) {
                    showDismissSoftInputAlertDialog(this.mContext);
                    Secure.putInt(this.mContext.getContentResolver(), HAS_SHOW_DIALOG, 1);
                }
                this.mHasShow = true;
            }
        }
    }

    private void showDismissSoftInputAlertDialog(Context context) {
        Log.i(TAG, "showDismissSoftInputAlertDialog");
        if (this.mAlterSoftInputDialog == null || !this.mAlterSoftInputDialog.isShowing()) {
            Builder buider = new Builder(context, 33947691);
            View view = LayoutInflater.from(buider.getContext()).inflate(34013311, null);
            if (view != null) {
                ImageView imageView = (ImageView) view.findViewById(34603234);
                TextView textView = (TextView) view.findViewById(34603232);
                if (imageView != null && textView != null) {
                    imageView.setImageResource(33751966);
                    textView.setText(context.getResources().getString(33686120));
                    this.mAlterSoftInputDialog = buider.setTitle(33686122).setPositiveButton(33686121, -$$Lambda$HwInputManagerServiceEx$3Xt6EEjK_GXDs03-sW6_dm0X4uo.INSTANCE).setView(view).create();
                    this.mAlterSoftInputDialog.getWindow().setType(HwArbitrationDEFS.MSG_MPLINK_BIND_FAIL);
                    this.mAlterSoftInputDialog.show();
                }
            }
        }
    }

    private void setTHPCallback(HALCallback callback) {
        synchronized (this.mLock) {
            try {
                if (this.mTPHal != null) {
                    if (DEBUG) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("setTHPCallback hwTsSetCallback callback = ");
                        stringBuilder.append(callback);
                        Slog.i(str, stringBuilder.toString());
                    }
                    this.mTPHal.hwTsSetCallback(callback);
                } else if (DEBUG) {
                    Slog.i(TAG, "setTHPCallback failed, TP service has not been initialized yet !!");
                }
            } catch (Exception e) {
                String str2 = TAG;
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("hwTsSetCallback Exception e = ");
                stringBuilder2.append(e);
                Slog.e(str2, stringBuilder2.toString());
            }
        }
    }
}
