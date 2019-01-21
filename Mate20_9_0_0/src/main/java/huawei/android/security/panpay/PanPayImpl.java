package huawei.android.security.panpay;

import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import huawei.android.security.IHwSecurityService;
import huawei.android.security.IHwSecurityService.Stub;

public class PanPayImpl {
    private static final int PAN_PAY_PLUGIN_ID = 12;
    private static final String SECURITY_SERVICE = "securityserver";
    private static final String TAG = "PanPayImpl";
    private static final Object mInstanceSync = new Object();
    private static IHwSecurityService mSecurityService;
    private static IPanPay sPanPayManager;
    private static volatile PanPayImpl sSelf = null;

    private PanPayImpl() {
    }

    public static PanPayImpl getInstance() {
        if (sSelf == null) {
            synchronized (PanPayImpl.class) {
                if (sSelf == null) {
                    sSelf = new PanPayImpl();
                    mSecurityService = Stub.asInterface(ServiceManager.getService(SECURITY_SERVICE));
                    if (mSecurityService == null) {
                        Log.e(TAG, "error, securityserver was null");
                    }
                }
            }
        }
        return sSelf;
    }

    private IPanPay getPanPayManagerService() {
        synchronized (mInstanceSync) {
            if (sPanPayManager != null) {
                IPanPay iPanPay = sPanPayManager;
                return iPanPay;
            }
            if (mSecurityService != null) {
                try {
                    sPanPayManager = IPanPay.Stub.asInterface(mSecurityService.querySecurityInterface(12));
                    IPanPay iPanPay2 = sPanPayManager;
                    return iPanPay2;
                } catch (RemoteException e) {
                    Log.e(TAG, "Get getPanPayManagerService failed!");
                }
            }
            return null;
        }
    }

    public int updateAppInfo(String type, String key, IPanPayCallBack callback) {
        if (getPanPayManagerService() != null) {
            try {
                sPanPayManager.updateAppInfo(type, key, callback);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while updateAppInfo");
                return 1;
            }
        }
        return 0;
    }

    public int operateAppInfo(String type, String key, IPanPayCallBack callback) {
        if (getPanPayManagerService() != null) {
            try {
                sPanPayManager.operateAppInfo(type, key, callback);
            } catch (RemoteException e) {
                Log.e(TAG, "RemoteException while operateApp");
                return 1;
            }
        }
        return 0;
    }
}
