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
        private final NetworkScanRequest mRequest;

        NetworkScanInfo(NetworkScanRequest request, NetworkScanCallback callback) {
            this.mRequest = request;
            this.mCallback = callback;
        }
    }

    public TelephonyScanManager() {
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        this.mLooper = thread.getLooper();
        this.mMessenger = new Messenger(new Handler(this.mLooper) {
            public void handleMessage(Message message) {
                Preconditions.checkNotNull(message, "message cannot be null");
                synchronized (TelephonyScanManager.this.mScanInfo) {
                    NetworkScanInfo nsi = (NetworkScanInfo) TelephonyScanManager.this.mScanInfo.get(message.arg2);
                }
                if (nsi == null) {
                    throw new RuntimeException("Failed to find NetworkScanInfo with id " + message.arg2);
                }
                NetworkScanCallback callback = nsi.mCallback;
                if (callback == null) {
                    throw new RuntimeException("Failed to find NetworkScanCallback with id " + message.arg2);
                }
                switch (message.what) {
                    case 1:
                        try {
                            Parcelable[] parcelables = message.getData().getParcelableArray(TelephonyScanManager.SCAN_RESULT_KEY);
                            CellInfo[] ci = new CellInfo[parcelables.length];
                            for (int i = 0; i < parcelables.length; i++) {
                                ci[i] = (CellInfo) parcelables[i];
                            }
                            callback.onResults(Arrays.asList(ci));
                            return;
                        } catch (Exception e) {
                            Rlog.e(TelephonyScanManager.TAG, "Exception in networkscan callback onResults", e);
                            return;
                        }
                    case 2:
                        try {
                            callback.onError(message.arg1);
                            return;
                        } catch (Exception e2) {
                            Rlog.e(TelephonyScanManager.TAG, "Exception in networkscan callback onError", e2);
                            return;
                        }
                    case 3:
                        try {
                            callback.onComplete();
                            TelephonyScanManager.this.mScanInfo.remove(message.arg2);
                            return;
                        } catch (Exception e22) {
                            Rlog.e(TelephonyScanManager.TAG, "Exception in networkscan callback onComplete", e22);
                            return;
                        }
                    default:
                        Rlog.e(TelephonyScanManager.TAG, "Unhandled message " + Integer.toHexString(message.what));
                        return;
                }
            }
        });
    }

    public NetworkScan requestNetworkScan(int subId, NetworkScanRequest request, NetworkScanCallback callback) {
        try {
            ITelephony telephony = getITelephony();
            if (telephony != null) {
                int scanId = telephony.requestNetworkScan(subId, request, this.mMessenger, new Binder());
                saveScanInfo(scanId, request, callback);
                return new NetworkScan(scanId, subId);
            }
        } catch (RemoteException ex) {
            Rlog.e(TAG, "requestNetworkScan RemoteException", ex);
        } catch (NullPointerException ex2) {
            Rlog.e(TAG, "requestNetworkScan NPE", ex2);
        }
        return null;
    }

    private void saveScanInfo(int id, NetworkScanRequest request, NetworkScanCallback callback) {
        synchronized (this.mScanInfo) {
            this.mScanInfo.put(id, new NetworkScanInfo(request, callback));
        }
    }

    private ITelephony getITelephony() {
        return Stub.asInterface(ServiceManager.getService("phone"));
    }
}
