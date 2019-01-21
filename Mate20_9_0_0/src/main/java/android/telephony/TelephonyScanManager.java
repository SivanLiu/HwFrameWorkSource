package android.telephony;

import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.Parcelable;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.SparseArray;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephony.Stub;
import com.android.internal.util.Preconditions;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

public final class TelephonyScanManager {
    public static final int CALLBACK_SCAN_COMPLETE = 3;
    public static final int CALLBACK_SCAN_ERROR = 2;
    public static final int CALLBACK_SCAN_RESULTS = 1;
    public static final String SCAN_RESULT_KEY = "scanResult";
    private static final String TAG = "TelephonyScanManager";
    private final Looper mLooper;
    private final Messenger mMessenger;
    private SparseArray<NetworkScanInfo> mScanInfo = new SparseArray();

    public static abstract class NetworkScanCallback {
        public void onResults(List<CellInfo> list) {
        }

        public void onComplete() {
        }

        public void onError(int error) {
        }
    }

    private static class NetworkScanInfo {
        private final NetworkScanCallback mCallback;
        private final Executor mExecutor;
        private final NetworkScanRequest mRequest;

        NetworkScanInfo(NetworkScanRequest request, Executor executor, NetworkScanCallback callback) {
            this.mRequest = request;
            this.mExecutor = executor;
            this.mCallback = callback;
        }
    }

    public TelephonyScanManager() {
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mLooper = thread.getLooper();
        this.mMessenger = new Messenger(new Handler(this.mLooper) {
            public void handleMessage(Message message) {
                NetworkScanInfo nsi;
                Preconditions.checkNotNull(message, "message cannot be null");
                synchronized (TelephonyScanManager.this.mScanInfo) {
                    nsi = (NetworkScanInfo) TelephonyScanManager.this.mScanInfo.get(message.arg2);
                }
                if (nsi != null) {
                    NetworkScanCallback callback = nsi.mCallback;
                    Executor executor = nsi.mExecutor;
                    StringBuilder stringBuilder;
                    if (callback == null) {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to find NetworkScanCallback with id ");
                        stringBuilder.append(message.arg2);
                        throw new RuntimeException(stringBuilder.toString());
                    } else if (executor != null) {
                        switch (message.what) {
                            case 1:
                                try {
                                    Parcelable[] parcelables = message.getData().getParcelableArray(TelephonyScanManager.SCAN_RESULT_KEY);
                                    CellInfo[] ci = new CellInfo[parcelables.length];
                                    for (int i = 0; i < parcelables.length; i++) {
                                        ci[i] = (CellInfo) parcelables[i];
                                    }
                                    executor.execute(new -$$Lambda$TelephonyScanManager$1$jmXulbd8FzO5Qb8_Hi-Z6s_nJWI(ci, callback));
                                    return;
                                } catch (Exception e) {
                                    Rlog.e(TelephonyScanManager.TAG, "Exception in networkscan callback onResults", e);
                                    return;
                                }
                            case 2:
                                try {
                                    executor.execute(new -$$Lambda$TelephonyScanManager$1$X9SMshZoHjJ6SzCbmgVMwQip2Q0(message.arg1, callback));
                                    return;
                                } catch (Exception e2) {
                                    Rlog.e(TelephonyScanManager.TAG, "Exception in networkscan callback onError", e2);
                                    return;
                                }
                            case 3:
                                try {
                                    executor.execute(new -$$Lambda$TelephonyScanManager$1$tGSpVQaVhc4GKIxjcECV-jCGYw4(callback));
                                    TelephonyScanManager.this.mScanInfo.remove(message.arg2);
                                    return;
                                } catch (Exception e22) {
                                    Rlog.e(TelephonyScanManager.TAG, "Exception in networkscan callback onComplete", e22);
                                    return;
                                }
                            default:
                                String str = TelephonyScanManager.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("Unhandled message ");
                                stringBuilder.append(Integer.toHexString(message.what));
                                Rlog.e(str, stringBuilder.toString());
                                return;
                        }
                    } else {
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Failed to find Executor with id ");
                        stringBuilder.append(message.arg2);
                        throw new RuntimeException(stringBuilder.toString());
                    }
                }
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Failed to find NetworkScanInfo with id ");
                stringBuilder2.append(message.arg2);
                throw new RuntimeException(stringBuilder2.toString());
            }

            static /* synthetic */ void lambda$handleMessage$0(CellInfo[] ci, NetworkScanCallback callback) {
                String str = TelephonyScanManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onResults: ");
                stringBuilder.append(ci.toString());
                Rlog.d(str, stringBuilder.toString());
                callback.onResults(Arrays.asList(ci));
            }

            static /* synthetic */ void lambda$handleMessage$1(int errorCode, NetworkScanCallback callback) {
                String str = TelephonyScanManager.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("onError: ");
                stringBuilder.append(errorCode);
                Rlog.d(str, stringBuilder.toString());
                callback.onError(errorCode);
            }

            static /* synthetic */ void lambda$handleMessage$2(NetworkScanCallback callback) {
                Rlog.d(TelephonyScanManager.TAG, "onComplete");
                callback.onComplete();
            }
        });
    }

    public NetworkScan requestNetworkScan(int subId, NetworkScanRequest request, Executor executor, NetworkScanCallback callback) {
        try {
            ITelephony telephony = getITelephony();
            if (telephony != null) {
                int scanId = telephony.requestNetworkScan(subId, request, this.mMessenger, new Binder());
                saveScanInfo(scanId, request, executor, callback);
                return new NetworkScan(scanId, subId);
            }
        } catch (RemoteException ex) {
            Rlog.e(TAG, "requestNetworkScan RemoteException", ex);
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "requestNetworkScan NPE", ex2);
        }
        return null;
    }

    private void saveScanInfo(int id, NetworkScanRequest request, Executor executor, NetworkScanCallback callback) {
        synchronized (this.mScanInfo) {
            this.mScanInfo.put(id, new NetworkScanInfo(request, executor, callback));
        }
    }

    private ITelephony getITelephony() {
        return Stub.asInterface(ServiceManager.getService("phone"));
    }
}
