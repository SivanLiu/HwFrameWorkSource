package android.telephony;

import android.os.RemoteException;
import android.os.ServiceManager;
import com.android.internal.telephony.ITelephony;
import com.android.internal.telephony.ITelephony.Stub;

public class NetworkScan {
    public static final int ERROR_INTERRUPTED = 10002;
    public static final int ERROR_INVALID_SCAN = 2;
    public static final int ERROR_INVALID_SCANID = 10001;
    public static final int ERROR_MODEM_BUSY = 3;
    public static final int ERROR_MODEM_ERROR = 1;
    public static final int ERROR_RIL_ERROR = 10000;
    public static final int ERROR_UNSUPPORTED = 4;
    public static final int SUCCESS = 0;
    public static final String TAG = "NetworkScan";
    private final int mScanId;
    private final int mSubId;

    public void stop() throws RemoteException {
        try {
            ITelephony telephony = getITelephony();
            if (telephony != null) {
                telephony.stopNetworkScan(this.mSubId, this.mScanId);
                return;
            }
            throw new RemoteException("Failed to get the ITelephony instance.");
        } catch (RemoteException ex) {
            Rlog.e(TAG, "stopNetworkScan  RemoteException", ex);
            throw new RemoteException("Failed to stop the network scan with id " + this.mScanId);
        }
    }

    public NetworkScan(int scanId, int subId) {
        this.mScanId = scanId;
        this.mSubId = subId;
    }

    private ITelephony getITelephony() {
        return Stub.asInterface(ServiceManager.getService("phone"));
    }
}
