package android.app.timezone;

import android.app.timezone.IRulesManager.Stub;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.io.IOException;
import java.util.Arrays;

public final class RulesManager {
    private static final boolean DEBUG = false;
    public static final int ERROR_OPERATION_IN_PROGRESS = 1;
    public static final int ERROR_UNKNOWN_FAILURE = 2;
    public static final int SUCCESS = 0;
    private static final String TAG = "timezone.RulesManager";
    private final Context mContext;
    private final IRulesManager mIRulesManager = Stub.asInterface(ServiceManager.getService(Context.TIME_ZONE_RULES_MANAGER_SERVICE));

    private class CallbackWrapper extends ICallback.Stub {
        final Callback mCallback;
        final Handler mHandler;

        CallbackWrapper(Context context, Callback callback) {
            this.mCallback = callback;
            this.mHandler = new Handler(context.getMainLooper());
        }

        public void onFinished(int status) {
            RulesManager.logDebug("mCallback.onFinished(status), status=" + status);
            this.mHandler.post(new -$Lambda$XGnGFnwDfPWgxse09CN983EaD_Q(status, this));
        }

        /* synthetic */ void lambda$-android_app_timezone_RulesManager$CallbackWrapper_7810(int status) {
            this.mCallback.onFinished(status);
        }
    }

    public RulesManager(Context context) {
        this.mContext = context;
    }

    public RulesState getRulesState() {
        try {
            logDebug("sIRulesManager.getRulesState()");
            RulesState rulesState = this.mIRulesManager.getRulesState();
            logDebug("sIRulesManager.getRulesState() returned " + rulesState);
            return rulesState;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int requestInstall(ParcelFileDescriptor distroFileDescriptor, byte[] checkToken, Callback callback) throws IOException {
        ICallback iCallback = new CallbackWrapper(this.mContext, callback);
        try {
            logDebug("sIRulesManager.requestInstall()");
            return this.mIRulesManager.requestInstall(distroFileDescriptor, checkToken, iCallback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int requestUninstall(byte[] checkToken, Callback callback) {
        ICallback iCallback = new CallbackWrapper(this.mContext, callback);
        try {
            logDebug("sIRulesManager.requestUninstall()");
            return this.mIRulesManager.requestUninstall(checkToken, iCallback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestNothing(byte[] checkToken, boolean succeeded) {
        try {
            logDebug("sIRulesManager.requestNothing() with token=" + Arrays.toString(checkToken));
            this.mIRulesManager.requestNothing(checkToken, succeeded);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    static void logDebug(String msg) {
    }
}
