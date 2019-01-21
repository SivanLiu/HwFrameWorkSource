package android.app.timezone;

import android.app.timezone.IRulesManager.Stub;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;

public final class RulesManager {
    public static final String ACTION_RULES_UPDATE_OPERATION = "com.android.intent.action.timezone.RULES_UPDATE_OPERATION";
    private static final boolean DEBUG = false;
    public static final int ERROR_OPERATION_IN_PROGRESS = 1;
    public static final int ERROR_UNKNOWN_FAILURE = 2;
    public static final String EXTRA_OPERATION_STAGED = "staged";
    public static final int SUCCESS = 0;
    private static final String TAG = "timezone.RulesManager";
    private final Context mContext;
    private final IRulesManager mIRulesManager = Stub.asInterface(ServiceManager.getService(Context.TIME_ZONE_RULES_MANAGER_SERVICE));

    @Retention(RetentionPolicy.SOURCE)
    public @interface ResultCode {
    }

    private class CallbackWrapper extends ICallback.Stub {
        final Callback mCallback;
        final Handler mHandler;

        CallbackWrapper(Context context, Callback callback) {
            this.mCallback = callback;
            this.mHandler = new Handler(context.getMainLooper());
        }

        public void onFinished(int status) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mCallback.onFinished(status), status=");
            stringBuilder.append(status);
            RulesManager.logDebug(stringBuilder.toString());
            this.mHandler.post(new -$$Lambda$RulesManager$CallbackWrapper$t7a48uTTxaRuSo3YBKxBIbPQznY(this, status));
        }
    }

    public RulesManager(Context context) {
        this.mContext = context;
    }

    public RulesState getRulesState() {
        try {
            logDebug("mIRulesManager.getRulesState()");
            RulesState rulesState = this.mIRulesManager.getRulesState();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mIRulesManager.getRulesState() returned ");
            stringBuilder.append(rulesState);
            logDebug(stringBuilder.toString());
            return rulesState;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int requestInstall(ParcelFileDescriptor distroFileDescriptor, byte[] checkToken, Callback callback) throws IOException {
        ICallback iCallback = new CallbackWrapper(this.mContext, callback);
        try {
            logDebug("mIRulesManager.requestInstall()");
            return this.mIRulesManager.requestInstall(distroFileDescriptor, checkToken, iCallback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int requestUninstall(byte[] checkToken, Callback callback) {
        ICallback iCallback = new CallbackWrapper(this.mContext, callback);
        try {
            logDebug("mIRulesManager.requestUninstall()");
            return this.mIRulesManager.requestUninstall(checkToken, iCallback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void requestNothing(byte[] checkToken, boolean succeeded) {
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("mIRulesManager.requestNothing() with token=");
            stringBuilder.append(Arrays.toString(checkToken));
            logDebug(stringBuilder.toString());
            this.mIRulesManager.requestNothing(checkToken, succeeded);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    static void logDebug(String msg) {
    }
}
