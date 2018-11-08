package android.hishow;

import android.hishow.IHwHiShowManager.Stub;
import android.os.ServiceManager;
import android.util.Log;
import android.util.Singleton;

public class HwHiShowManager {
    private static final String SVC_NAME = "HwHiShowManagerService";
    private static final String TAG = "HwHiShowManager";
    private static final Singleton<IHwHiShowManager> iHwHiShowManagerSingleton = new Singleton<IHwHiShowManager>() {
        protected IHwHiShowManager create() {
            IHwHiShowManager hsm = Stub.asInterface(ServiceManager.getService(HwHiShowManager.SVC_NAME));
            Log.d(HwHiShowManager.TAG, "GS:create hsm is " + hsm);
            return hsm;
        }
    };

    public static IHwHiShowManager getService() {
        Log.d(TAG, "GS:enter");
        return (IHwHiShowManager) iHwHiShowManagerSingleton.get();
    }
}
