package android.hardware.location;

import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.content.Context;
import android.hardware.location.ContextHubTransaction.Response;
import android.hardware.location.IContextHubCallback.Stub;
import android.os.Handler;
import android.os.HandlerExecutor;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceManager.ServiceNotFoundException;
import android.util.Log;
import com.android.internal.util.Preconditions;
import java.util.List;
import java.util.concurrent.Executor;

@SystemApi
public final class ContextHubManager {
    private static final String TAG = "ContextHubManager";
    private Callback mCallback;
    private Handler mCallbackHandler;
    private final Stub mClientCallback = new Stub() {
        public void onMessageReceipt(int hubId, int nanoAppId, ContextHubMessage message) {
            if (ContextHubManager.this.mCallback != null) {
                synchronized (this) {
                    final Callback callback = ContextHubManager.this.mCallback;
                    final int i = hubId;
                    final int i2 = nanoAppId;
                    final ContextHubMessage contextHubMessage = message;
                    (ContextHubManager.this.mCallbackHandler == null ? new Handler(ContextHubManager.this.mMainLooper) : ContextHubManager.this.mCallbackHandler).post(new Runnable() {
                        public void run() {
                            callback.onMessageReceipt(i, i2, contextHubMessage);
                        }
                    });
                }
            } else if (ContextHubManager.this.mLocalCallback != null) {
                synchronized (this) {
                    ContextHubManager.this.mLocalCallback.onMessageReceipt(hubId, nanoAppId, message);
                }
            }
        }
    };
    @Deprecated
    private ICallback mLocalCallback;
    private final Looper mMainLooper;
    private final IContextHubService mService;

    @Deprecated
    public static abstract class Callback {
        public abstract void onMessageReceipt(int i, int i2, ContextHubMessage contextHubMessage);

        protected Callback() {
        }
    }

    @Deprecated
    public interface ICallback {
        void onMessageReceipt(int i, int i2, ContextHubMessage contextHubMessage);
    }

    @Deprecated
    public int[] getContextHubHandles() {
        try {
            return this.mService.getContextHubHandles();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public ContextHubInfo getContextHubInfo(int hubHandle) {
        try {
            return this.mService.getContextHubInfo(hubHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int loadNanoApp(int hubHandle, NanoApp app) {
        try {
            return this.mService.loadNanoApp(hubHandle, app);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int unloadNanoApp(int nanoAppHandle) {
        try {
            return this.mService.unloadNanoApp(nanoAppHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public NanoAppInstanceInfo getNanoAppInstanceInfo(int nanoAppHandle) {
        try {
            return this.mService.getNanoAppInstanceInfo(nanoAppHandle);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int[] findNanoAppOnHub(int hubHandle, NanoAppFilter filter) {
        try {
            return this.mService.findNanoAppOnHub(hubHandle, filter);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @Deprecated
    public int sendMessage(int hubHandle, int nanoAppHandle, ContextHubMessage message) {
        try {
            return this.mService.sendMessage(hubHandle, nanoAppHandle, message);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public List<ContextHubInfo> getContextHubs() {
        try {
            return this.mService.getContextHubs();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private IContextHubTransactionCallback createTransactionCallback(final ContextHubTransaction<Void> transaction) {
        return new IContextHubTransactionCallback.Stub() {
            public void onQueryResponse(int result, List<NanoAppState> list) {
                Log.e(ContextHubManager.TAG, "Received a query callback on a non-query request");
                transaction.setResponse(new Response(7, null));
            }

            public void onTransactionComplete(int result) {
                transaction.setResponse(new Response(result, null));
            }
        };
    }

    private IContextHubTransactionCallback createQueryCallback(final ContextHubTransaction<List<NanoAppState>> transaction) {
        return new IContextHubTransactionCallback.Stub() {
            public void onQueryResponse(int result, List<NanoAppState> nanoappList) {
                transaction.setResponse(new Response(result, nanoappList));
            }

            public void onTransactionComplete(int result) {
                Log.e(ContextHubManager.TAG, "Received a non-query callback on a query request");
                transaction.setResponse(new Response(7, null));
            }
        };
    }

    public ContextHubTransaction<Void> loadNanoApp(ContextHubInfo hubInfo, NanoAppBinary appBinary) {
        Preconditions.checkNotNull(hubInfo, "ContextHubInfo cannot be null");
        Preconditions.checkNotNull(appBinary, "NanoAppBinary cannot be null");
        ContextHubTransaction<Void> transaction = new ContextHubTransaction(0);
        try {
            this.mService.loadNanoAppOnHub(hubInfo.getId(), createTransactionCallback(transaction), appBinary);
            return transaction;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ContextHubTransaction<Void> unloadNanoApp(ContextHubInfo hubInfo, long nanoAppId) {
        Preconditions.checkNotNull(hubInfo, "ContextHubInfo cannot be null");
        ContextHubTransaction<Void> transaction = new ContextHubTransaction(1);
        try {
            this.mService.unloadNanoAppFromHub(hubInfo.getId(), createTransactionCallback(transaction), nanoAppId);
            return transaction;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ContextHubTransaction<Void> enableNanoApp(ContextHubInfo hubInfo, long nanoAppId) {
        Preconditions.checkNotNull(hubInfo, "ContextHubInfo cannot be null");
        ContextHubTransaction<Void> transaction = new ContextHubTransaction(2);
        try {
            this.mService.enableNanoApp(hubInfo.getId(), createTransactionCallback(transaction), nanoAppId);
            return transaction;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ContextHubTransaction<Void> disableNanoApp(ContextHubInfo hubInfo, long nanoAppId) {
        Preconditions.checkNotNull(hubInfo, "ContextHubInfo cannot be null");
        ContextHubTransaction<Void> transaction = new ContextHubTransaction(3);
        try {
            this.mService.disableNanoApp(hubInfo.getId(), createTransactionCallback(transaction), nanoAppId);
            return transaction;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ContextHubTransaction<List<NanoAppState>> queryNanoApps(ContextHubInfo hubInfo) {
        Preconditions.checkNotNull(hubInfo, "ContextHubInfo cannot be null");
        ContextHubTransaction<List<NanoAppState>> transaction = new ContextHubTransaction(4);
        try {
            this.mService.queryNanoApps(hubInfo.getId(), createQueryCallback(transaction));
            return transaction;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public int registerCallback(Callback callback) {
        return registerCallback(callback, null);
    }

    @Deprecated
    public int registerCallback(ICallback callback) {
        if (this.mLocalCallback != null) {
            Log.w(TAG, "Max number of local callbacks reached!");
            return -1;
        }
        this.mLocalCallback = callback;
        return 0;
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public int registerCallback(Callback callback, Handler handler) {
        synchronized (this) {
            if (this.mCallback != null) {
                Log.w(TAG, "Max number of callbacks reached!");
                return -1;
            }
            this.mCallback = callback;
            this.mCallbackHandler = handler;
            return 0;
        }
    }

    private IContextHubClientCallback createClientCallback(final ContextHubClient client, final ContextHubClientCallback callback, final Executor executor) {
        return new IContextHubClientCallback.Stub() {
            public void onMessageFromNanoApp(NanoAppMessage message) {
                executor.execute(new -$$Lambda$ContextHubManager$3$U9x_HK_GdADIEQ3mS5mDWMNWMu8(callback, client, message));
            }

            public void onHubReset() {
                executor.execute(new -$$Lambda$ContextHubManager$3$kLhhBRChCeue1LKohd5lK_lfKTU(callback, client));
            }

            public void onNanoAppAborted(long nanoAppId, int abortCode) {
                executor.execute(new -$$Lambda$ContextHubManager$3$hASoxw9hzmd9l2NpC91O5tXLzxU(callback, client, nanoAppId, abortCode));
            }

            public void onNanoAppLoaded(long nanoAppId) {
                executor.execute(new -$$Lambda$ContextHubManager$3$5yx25kUuvL9qy3uBcIzI3sQQoL8(callback, client, nanoAppId));
            }

            public void onNanoAppUnloaded(long nanoAppId) {
                executor.execute(new -$$Lambda$ContextHubManager$3$KgVQePwT_QpjU9EQTp2L3LsHE5Y(callback, client, nanoAppId));
            }

            public void onNanoAppEnabled(long nanoAppId) {
                executor.execute(new -$$Lambda$ContextHubManager$3$8oeFzBAC_VuH1d32Kod8BVn0Os8(callback, client, nanoAppId));
            }

            public void onNanoAppDisabled(long nanoAppId) {
                executor.execute(new -$$Lambda$ContextHubManager$3$On2Q5Obzm4-zLY0UP3Xs4E3P-V0(callback, client, nanoAppId));
            }
        };
    }

    public ContextHubClient createClient(ContextHubInfo hubInfo, ContextHubClientCallback callback, Executor executor) {
        Preconditions.checkNotNull(callback, "Callback cannot be null");
        Preconditions.checkNotNull(hubInfo, "ContextHubInfo cannot be null");
        Preconditions.checkNotNull(executor, "Executor cannot be null");
        ContextHubClient client = new ContextHubClient(hubInfo);
        try {
            client.setClientProxy(this.mService.createClient(createClientCallback(client, callback, executor), hubInfo.getId()));
            return client;
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public ContextHubClient createClient(ContextHubInfo hubInfo, ContextHubClientCallback callback) {
        return createClient(hubInfo, callback, new HandlerExecutor(Handler.getMain()));
    }

    @SuppressLint({"Doclava125"})
    @Deprecated
    public int unregisterCallback(Callback callback) {
        synchronized (this) {
            if (callback != this.mCallback) {
                Log.w(TAG, "Cannot recognize callback!");
                return -1;
            }
            this.mCallback = null;
            this.mCallbackHandler = null;
            return 0;
        }
    }

    @Deprecated
    public synchronized int unregisterCallback(ICallback callback) {
        if (callback != this.mLocalCallback) {
            Log.w(TAG, "Cannot recognize local callback!");
            return -1;
        }
        this.mLocalCallback = null;
        return 0;
    }

    public ContextHubManager(Context context, Looper mainLooper) throws ServiceNotFoundException {
        this.mMainLooper = mainLooper;
        this.mService = IContextHubService.Stub.asInterface(ServiceManager.getServiceOrThrow(Context.CONTEXTHUB_SERVICE));
        try {
            this.mService.registerCallback(this.mClientCallback);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
